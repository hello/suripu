package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.bayes.BetaDiscreteWithEventOutput;
import com.hello.suripu.algorithm.bayes.BetaBinomialBayesModel;
import com.hello.suripu.algorithm.bayes.ModelWithDiscreteProbabiltiesAndEventOccurence;
import com.hello.suripu.algorithm.bayes.MultipleEventModel;
import com.hello.suripu.algorithm.bayes.SensorDataReductionAndInterpretation;
import com.hello.suripu.algorithm.hmm.ChiSquarePdf;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import com.hello.suripu.algorithm.hmm.GammaPdf;
import com.hello.suripu.algorithm.hmm.GaussianPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.api.datascience.SleepHmmBayesNetProtos;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 6/21/15.
 */
public class HmmBayesNetDeserialization {
    final static int MAX_NUM_HMM_STATES = 500;
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(HmmBayesNetDeserialization.class);
    private final Logger LOGGER;
    /*
    *  Goal is to create a bunch of HMMs, which decode some sensor data.
    *  The state sequences map to a bunch of conditional probabilities conditioned on a binary probability
    *  of some occurrence (on bed, sleeping, etc.).  Binary probability = ( P, !P)
    *
    *  Conditional probabilities?  It'll be a Beta-discrete implementation of a MultipleEventModel
    *  It'll live in a map, where the key is the model it came from
    *
    *
    * */

    private final SleepHmmBayesNetProtos.HmmBayesNet proto;

    public static class DeserializedSleepHmmBayesNetWithParams {
        public final SensorDataReductionAndInterpretation sensorDataReductionAndInterpretation;
        public final HmmBayesNetMeasurementParameters params;

        public DeserializedSleepHmmBayesNetWithParams(SensorDataReductionAndInterpretation sensorDataReductionAndInterpretation, HmmBayesNetMeasurementParameters params) {
            this.sensorDataReductionAndInterpretation = sensorDataReductionAndInterpretation;
            this.params = params;
        }
    }

    public HmmBayesNetDeserialization(final SleepHmmBayesNetProtos.HmmBayesNet proto, Optional<UUID> uuid) {
        this.proto = proto;
        this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER, uuid);
    }

    public DeserializedSleepHmmBayesNetWithParams Deserialize() {
        final SleepHmmBayesNetProtos.MeasurementParams params = proto.getMeasurementParameters();

        /*  FIRST MAKE THE HMMS */
        final Map<String,HiddenMarkovModel> hmmMapById = Maps.newHashMap();

        for (SleepHmmBayesNetProtos.HiddenMarkovModel model : proto.getIndependentHmmsList()) {
            hmmMapById.put(model.getId(),getHmm(model));
        }

        /* SECOND, MAKE THE CONDITIONAL PROBABILITY MODELS  */
        final Map<String,String> modelNamesToOutputNames = Maps.newHashMap();
        final Map<String,MultipleEventModel> interpretationByOutputName = Maps.newHashMap();

        for (final SleepHmmBayesNetProtos.CondProbs condProbs : proto.getConditionalProbabilitiesList()) {
            final String modelId = condProbs.getModelId();
            final String outputId = condProbs.getOutputId();

            //map of input to output
            modelNamesToOutputNames.put(modelId, outputId);


            //deserialize beta distributions (representing conditional probabilities)
            final List<ModelWithDiscreteProbabiltiesAndEventOccurence> condProbsByEvent = getConditionalProbabilityModel(condProbs);

            //populate defaults
            if (interpretationByOutputName.get(outputId) == null) {
                interpretationByOutputName.put(outputId,new MultipleEventModel(2)); //binary distriubtions -- 2 discrete probs
            }

            interpretationByOutputName.get(outputId).addModel(modelId,condProbsByEvent);
        }

        /* LAST, EXTRACT THE MEAUREMENT PARAMS OF THIS MODEL */
    
        return new DeserializedSleepHmmBayesNetWithParams(
                new SensorDataReductionAndInterpretation(hmmMapById,interpretationByOutputName,modelNamesToOutputNames),
                HmmBayesNetMeasurementParameters.createFromProto(proto.getMeasurementParameters()));
    }

    private  List<ModelWithDiscreteProbabiltiesAndEventOccurence> getConditionalProbabilityModel(final SleepHmmBayesNetProtos.CondProbs condProbs) {
        List<ModelWithDiscreteProbabiltiesAndEventOccurence> condProbModel = Lists.newArrayList();
        for (SleepHmmBayesNetProtos.BetaCondProb betaCondProb : condProbs.getProbsList()) {

            //create binary complementary model (p, not p) and you can do this with the beta distribution by swapping alpha and beta
            final BetaBinomialBayesModel betaBinomialBayesModel = new BetaBinomialBayesModel(betaCondProb.getAlpha(),betaCondProb.getBeta());
            final BetaBinomialBayesModel betaBinomialBayesModelComplementary = new BetaBinomialBayesModel(betaCondProb.getBeta(),betaCondProb.getAlpha());
            final BetaDiscreteWithEventOutput betaDiscreteWithEventOutput = new BetaDiscreteWithEventOutput(Lists.newArrayList(betaBinomialBayesModel, betaBinomialBayesModelComplementary));

            condProbModel.add(betaDiscreteWithEventOutput);
        }


        return condProbModel;

    }

    private  HiddenMarkovModel getHmm(final SleepHmmBayesNetProtos.HiddenMarkovModel model) {
        final int numStates = model.getNumStates();
        final double [][] A = getMatrix(model.getStateTransitionMatrixList(),numStates);

        final double [] initProbs = new double[numStates];
        Arrays.fill(initProbs,1.0);

        final HmmPdfInterface [] models = getObsModels(model.getObservationModelList(),numStates);

        int numFreeParams = 0;
        for (HmmPdfInterface m : models) {
            numFreeParams += m.getNumFreeParams();
        }

        HiddenMarkovModel hmm = new HiddenMarkovModel(numStates,A,initProbs,models,numFreeParams);

        return hmm;
    }

    private  HmmPdfInterface [] getObsModels(List<SleepHmmBayesNetProtos.ObsModel> obsModels, final int numStates) {

        final Map<Integer,PdfComposite> composites = Maps.newHashMap();

        int highestStateNumber = 0;
        int count = 0;
        for (SleepHmmBayesNetProtos.ObsModel model : obsModels) {
            final Optional<HmmPdfInterface> pdf = getPdfFromObsModel(model);

            if (!pdf.isPresent()) {
                LOGGER.error("did not find a valid observation model number {}",count);
                continue;
            }

            final int stateIdx = model.getStateIndex();
            if (composites.get(stateIdx) == null) {
                composites.put(stateIdx,new PdfComposite());
            }

            composites.get(stateIdx).addPdf(pdf.get());

            if (model.getStateIndex() > highestStateNumber) {
                highestStateNumber = model.getStateIndex();
            }

            count++;
        }

        if (highestStateNumber >= numStates) {
            return new HmmPdfInterface[0];
        }

        HmmPdfInterface [] pdfs = new HmmPdfInterface[numStates];
        for (Integer iState = 0; iState < numStates; iState++) {

            if (composites.get(iState) == null) {
                LOGGER.error("No models found for state {}, which means it is a gap (every state index should have a model, which means you overlooked something when you made the protobuf");
                //no gaps!
                return new HmmPdfInterface[0];
            }

            pdfs[iState] = composites.get(iState);


        }

        return pdfs;
    }

    private  double [][] getMatrix(final List<Double> matAsVec, final int ncols) {
        final int nrows = matAsVec.size() / ncols;

        double [][] mat = new double[nrows][ncols];

        for (int j = 0; j < nrows; j++) {
            for (int i = 0; i < ncols; i++) {
                final int idx = j * ncols + i;
                mat[j][i] = matAsVec.get(idx);
            }
        }

        return mat;
    }

    private  Optional<HmmPdfInterface> getPdfFromObsModel(final SleepHmmBayesNetProtos.ObsModel obsModel) {

        final int measNumber = obsModel.getMeasType().getNumber();

        if (obsModel.hasGaussian()) {
            return Optional.of((HmmPdfInterface)new GaussianPdf(obsModel.getGaussian().getMean(),obsModel.getGaussian().getStddev(),measNumber));
        }

        if (obsModel.hasAlphabet()) {
            final int N = obsModel.getAlphabet().getProbabilitiesCount();
            final List<Double> alphabetProbs = Lists.newArrayList();

            for (int i = 0; i < N; i++) {
                alphabetProbs.add(obsModel.getAlphabet().getProbabilities(i));
            }

            return Optional.of((HmmPdfInterface)new DiscreteAlphabetPdf(alphabetProbs,measNumber));
        }

        if (obsModel.hasChisquare()) {
            return Optional.of((HmmPdfInterface)new ChiSquarePdf(obsModel.getChisquare().getMean(),measNumber));
        }

        if (obsModel.hasGamma()) {
            return Optional.of((HmmPdfInterface)new GammaPdf(obsModel.getGamma().getMean(),obsModel.getGamma().getStddev(),measNumber));
        }

        if (obsModel.hasPoisson()) {
            return Optional.of((HmmPdfInterface)new PoissonPdf(obsModel.getPoisson().getMean(),measNumber));
        }


        return  Optional.absent();

    }
}

package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.algorithm.hmm.SensorDataReduction;
import com.hello.suripu.algorithm.hmm.ChiSquarePdf;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import com.hello.suripu.algorithm.hmm.GammaPdf;
import com.hello.suripu.algorithm.hmm.GaussianPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelFactory;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelInterface;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.api.datascience.SleepHmmBayesNetProtos;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 6/21/15.
 */
public class FeatureExtractionModelData {
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(FeatureExtractionModelData.class);
    private static final HiddenMarkovModelFactory.HmmType HMM_TYPE = HiddenMarkovModelFactory.HmmType.LOGMATH;
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

    private  Optional<DeserializedFeatureExtractionWithParams> deserializedData;

    public FeatureExtractionModelData(final Optional<UUID> uuid) {
        this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER, uuid);
        deserializedData = Optional.absent();
    }

    public DeserializedFeatureExtractionWithParams getDeserializedData() {
        return deserializedData.get();
    }

    public boolean isValid() {
        return this.deserializedData.isPresent();
    }


    public void deserialize(final byte [] serializedProtobufData) {

        try {
            final SleepHmmBayesNetProtos.HmmBayesNet proto = SleepHmmBayesNetProtos.HmmBayesNet.parseFrom(serializedProtobufData);

        /*  FIRST MAKE THE HMMS */
            final Map<String,HiddenMarkovModelInterface> hmmMapById = Maps.newHashMap();

            for (SleepHmmBayesNetProtos.HiddenMarkovModel model : proto.getIndependentHmmsList()) {
                hmmMapById.put(model.getId(),getHmm(model));
            }

        /* LAST, EXTRACT THE MEAUREMENT PARAMS OF THIS MODEL */

            deserializedData = Optional.of( new DeserializedFeatureExtractionWithParams(
                    new SensorDataReduction(hmmMapById),
                    OnlineHmmMeasurementParameters.createFromProto(proto.getMeasurementParameters())));

        } catch (InvalidProtocolBufferException e) {
            LOGGER.error(e.toString());
        }
    }


    /* create HMM from protobuf   */
    private  HiddenMarkovModelInterface getHmm(final SleepHmmBayesNetProtos.HiddenMarkovModel model) {
        final int numStates = model.getNumStates();
        final double [][] A = getMatrix(model.getStateTransitionMatrixList(),numStates);

        final double [] initProbs = new double[numStates];
        initProbs[0] = 1.0;

        final HmmPdfInterface [] models = getObsModels(model.getObservationModelList(),numStates);

        int numFreeParams = 0;
        for (HmmPdfInterface m : models) {
            numFreeParams += m.getNumFreeParams();
        }


        return HiddenMarkovModelFactory.create(HMM_TYPE,numStates,A,initProbs,models,numFreeParams);
    }

    /* create HMM obs models from protobuf data  */
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

    /* get a single pdf from obs model -- this is just a factory method */
    private  Optional<HmmPdfInterface> getPdfFromObsModel(final SleepHmmBayesNetProtos.ObsModel obsModel) {

        if (obsModel.getMeasTypeCount() == 0) {
            return Optional.absent();
        }

        //for now just get the first one, later we may have vector measurements
        final int measNumber = obsModel.getMeasType(0).getNumber();

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

package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.algorithm.bayes.SensorDataReduction;
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
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 6/21/15.
 */
public class FeatureExtractionModelData {
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(FeatureExtractionModelData.class);
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

    ///for testing purposes
    public void deserializeDefault() {
        deserialize(Base64.decodeBase64(DEFAULT_PROTOBUF));
    }







    public void deserialize(final byte [] serializedProtobufData) {

        try {
            final SleepHmmBayesNetProtos.HmmBayesNet proto = SleepHmmBayesNetProtos.HmmBayesNet.parseFrom(serializedProtobufData);


            final SleepHmmBayesNetProtos.MeasurementParams params = proto.getMeasurementParameters();

        /*  FIRST MAKE THE HMMS */
            final Map<String,HiddenMarkovModel> hmmMapById = Maps.newHashMap();

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

    //for testing purposes
    private static final String DEFAULT_PROTOBUF =
            "CiEIBRAAGQAAAAAAADBAIQAAAAAAABBAOAFBAAAAAABMzUASuy8KBm1vdGlvbhINLi9tb3Rpb24uanNvbhoPCAAQAUIJCXsUrkfheoQ/Gg8IARABQgkJpFTCE3p9/D8aDwgCEAFCCQmHinH+JihLQBoPCAMQAUIJCSb+KOrMvSpAGg8IBBABQgkJSIeHMH66EEAaDwgFEAFCCQkjvD0IAdkdQBoPCAYQAUIJCXsUrkfheoQ/Gg8IBxABQgkJzCVV202wD0AaDwgIEAFCCQkMWkjA6MNXQBoPCAkQAUIJCXVat0HtszxAGg8IChABQgkJ/tXjvtWgQEAaDwgLEAFCCQlBvK5fsOJkQBoPCAwQAUIJCVEtIorJm/g/Gg8IDRABQgkJBwq8k0/fGEAaDwgOEAFCCQniqx3FOfo6QBoPCA8QAUIJCajg8IKIVBdAGg8IEBABQgkJZ0P+mUGcHUAaDwgREAFCCQlnnlxTIAsuQBoPCBIQAUIJCUEtBg/TBixAGg8IExABQgkJ3gIJih9jIkAaDwgUEAFCCQmlaybfbKMWQBoPCBUQAUIJCfaWcr7YOzRAGg8IFhABQgkJHt5zYDnaIkAaDwgXEAFCCQkmGTkLe6oVQBoPCBgQAUIJCaj8a3nlajZAIbFppRDIpe8/IVVKdDugEWE/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IbWv8ro0NGQ/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IfcGX5hMFdQ/IUhuTbotkdQ/IQte9BWkGYs/Ifyp8dJNYlA/Ifyp8dJNYlA/ISob1lQWhc0/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IWco7niT37I/Ickh4uZUMqg/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IZBBOLBmro0/ISm0rPvHQtY/Ifyp8dJNYlA/ITV7oBUYssA/IeGaO/pfrp0/ISm0rPvHQrY/IeGaO/pfrq0/ISm0rPvHQsY/IeGaO/pfrr0/IZBBOLBmro0/Ifyp8dJNYlA/IeGaO/pfrp0/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IYhJuJBHcL8/IYBSYSoF9oM/IbBW7ZqQ1sI/Ifyp8dJNYlA/IQ0dO6jEdaQ/IdsTJLa7h+A/Ifyp8dJNYlA/Ifyp8dJNYlA/IQolOJrIcaw/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Iayql99pMrs/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IYDVkSOdgdM/Ifyp8dJNYlA/Ifyp8dJNYlA/IUAVN24xP+Y/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ic2pCUc1kY4/Ie3T8ZiByuE/Ifyp8dJNYlA/IfD6zFmfcsY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IeqRBre1hb8/IeqRBre1hb8/IYfN15cykX4/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/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Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IcrFGFjHcew/IRzyKsZUVbU/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IQolOJrIcZw/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IcrFGFjHcew/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IdU3ekZswbY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/ISkO+5hqwZY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IfwBDwwgfOA/IQn84ee/B48/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IQd96e3PRac/Ifyp8dJNYlA/IYi85erHJts/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IctpT8k5saM/Ifyp8dJNYlA/IViP+1brxO4/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IU3XE10XfuU/Ifyp8dJNYlA/IXVWC+wxka4/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IQLsSJrk7KY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/ITAPmfIhqMw/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IcP1KFyPwuU/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IXsUrkfhetQ/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IZqZmZmZmck/ISkO+5hqwaY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IY19ycaDLeg/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IZqZmZmZmbk/Ifyp8dJNYlA/IZqZmZmZmbk/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IZqZmZmZmbk/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IWZmZmZmZuY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/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";
}

package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.algorithm.hmm.*;
import com.hello.suripu.api.datascience.OnlineHmmProtos;
import com.hello.suripu.api.datascience.OnlineHmmProtos.*;
import com.hello.suripu.api.datascience.OnlineHmmProtos.Transition;
import org.apache.commons.codec.binary.Base64;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/18/15.
 */
public class  OnlineHmmPriors {

    public final Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId;
    public final Multimap<String,com.hello.suripu.algorithm.hmm.Transition> forbiddenMotionTransitionsByOutputId;

    private OnlineHmmPriors(Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId, final Multimap<String,com.hello.suripu.algorithm.hmm.Transition> forbiddenMotionTransitions) {
        this.modelsByOutputId = modelsByOutputId;
        this.forbiddenMotionTransitionsByOutputId = forbiddenMotionTransitions;
    }

    @Override
    public OnlineHmmPriors clone() {
        final Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId = Maps.newHashMap();

        for (final String key : modelsByOutputId.keySet()) {
            final Map<String,OnlineHmmModelParams> myMap = Maps.newHashMap();

            for (final Map.Entry<String,OnlineHmmModelParams> params : modelsByOutputId.get(key).entrySet()) {
                myMap.put(params.getValue().id, params.getValue().clone());
            }

            modelsByOutputId.put(key, myMap);
        }

        final  Multimap<String,com.hello.suripu.algorithm.hmm.Transition> myMap = ArrayListMultimap.create();

        myMap.putAll(forbiddenMotionTransitionsByOutputId);

        return new OnlineHmmPriors(modelsByOutputId, myMap);
    }


    private static String protobufEnumToString(OnlineHmmProtos.OutputId outputId) {
        String outputIdString = "unknown";
        switch (outputId) {

            case SLEEP:
                outputIdString = OnlineHmmData.OUTPUT_MODEL_SLEEP;
                break;

            case BED:
                outputIdString = OnlineHmmData.OUTPUT_MODEL_BED;
                break;

            default:
                //LOGGER.error("BAD ENUM, BAD BAD ENUM")
                break;
        }

        return outputIdString;
    }

    private static double[][] getMatrix(final RealMatrix protobuf) {
        final double[][] mtx = new double[protobuf.getNumRows()][protobuf.getNumCols()];

        int k = 0;
        for (int j = 0; j < protobuf.getNumRows(); j++) {
            for (int i = 0; i < protobuf.getNumCols(); i++) {
                mtx[j][i] = protobuf.getData(k);
                k++;
            }
        }

        return mtx;
    }

    private static RealMatrix getProtobufMatrix(final double[][] mtx) {
        final List<Double> vec = Lists.newArrayList();

        final int numRows = mtx.length;
        final int numCols = mtx[0].length;

        for (int j = 0; j < numRows; j++) {
            for (int i = 0; i < numCols; i++) {
                vec.add(mtx[j][i]);
            }
        }

        return RealMatrix.newBuilder().addAllData(vec).setNumRows(numRows).setNumCols(numCols).build();
    }

    public static Optional<OnlineHmmModelParams> protobufToParams(final AlphabetHmmPrior protobuf) {

        //things that aren't really optional
        if (!protobuf.hasOutputId() ||
                !protobuf.hasOutputId() ||
                !protobuf.hasLogStateTransitionNumerator() ||
                protobuf.getLogDenominatorCount() == 0 ||
                protobuf.getLogObservationModelNumeratorCount() == 0 ||
                protobuf.getPiCount() == 0 ||
                protobuf.getEndStatesCount() == 0) {
            return Optional.absent();
        }


        long timeCreated = 0;
        long timeUpdated = 0;

        if (protobuf.hasDateCreatedUtc()) {
            timeCreated = protobuf.getDateCreatedUtc();
        }

        if (protobuf.hasDateUpdatedUtc()) {
            timeUpdated = protobuf.getDateUpdatedUtc();
        }

        final double[][] logStateTransition = getMatrix(protobuf.getLogStateTransitionNumerator());

        final double[] logDenominator = new double[protobuf.getLogDenominatorCount()];
        for (int i = 0; i < protobuf.getLogDenominatorCount(); i++) {
            logDenominator[i] = protobuf.getLogDenominator(i);
        }

        final double[] pi = new double[protobuf.getPiCount()];
        for (int i = 0; i < protobuf.getPiCount(); i++) {
            pi[i] = protobuf.getPi(i);
        }

        final int[] endStates = new int[protobuf.getEndStatesCount()];
        for (int i = 0; i < protobuf.getEndStatesCount(); i++) {
            endStates[i] = protobuf.getEndStates(i);
        }

        final int[] minStateDurations = new int[protobuf.getMinimumStateDurationsCount()];
        for (int i = 0; i < protobuf.getMinimumStateDurationsCount(); i++) {
            minStateDurations[i] = protobuf.getMinimumStateDurations(i);
        }

        final Map<String, double[][]> logAlphabetNumerators = Maps.newHashMap();

        for (int i = 0; i < protobuf.getLogObservationModelNumeratorCount(); i++) {
            final double[][] mtx = getMatrix(protobuf.getLogObservationModelNumerator(i));
            final String modelId = protobuf.getLogObservationModelIds(i);

            logAlphabetNumerators.put(modelId, mtx);
        }


        return Optional.of(new OnlineHmmModelParams(logAlphabetNumerators, logStateTransition, logDenominator, pi, endStates, minStateDurations, timeCreated, timeUpdated, protobuf.getId(), protobufEnumToString(protobuf.getOutputId())));

    }

    public static Optional<OnlineHmmPriors> createFromProtoBuf(final byte[] data) {
        try {
            final AlphabetHmmUserModel protobuf = AlphabetHmmUserModel.parseFrom(data);

            final Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId = Maps.newHashMap();

            for (final AlphabetHmmPrior prior : protobuf.getModelsList()) {
                final Optional<OnlineHmmModelParams> paramsOptional = protobufToParams(prior);

                if (!paramsOptional.isPresent()) {
                    continue;
                }

                final OnlineHmmModelParams params = paramsOptional.get();

                if (modelsByOutputId.get(params.outputId) == null) {
                    modelsByOutputId.put(params.outputId, Maps.<String, OnlineHmmModelParams>newHashMap());
                }

                modelsByOutputId.get(params.outputId).put(params.id, params);

            }


            Multimap<String,com.hello.suripu.algorithm.hmm.Transition> transitions = ArrayListMultimap.create();
            for (final Transition transition : protobuf.getForbiddedenMotionTransitionsList()) {
                transitions.put(protobufEnumToString(transition.getOutputId()),new com.hello.suripu.algorithm.hmm.Transition(transition.getFrom(), transition.getTo()));
            }

            return Optional.of(new OnlineHmmPriors(modelsByOutputId, transitions));


        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return Optional.absent();
        }
    }

    private static List<Double> getDoubleList(final double[] x) {
        List<Double> list = Lists.newArrayList();

        for (int i = 0; i < x.length; i++) {
            list.add(x[i]);
        }

        return list;
    }

    public static AlphabetHmmPrior protobufFromParams(final OnlineHmmModelParams params) {

        OutputId outputId = null;

        if (params.id.equals(OnlineHmmData.OUTPUT_MODEL_BED)) {
            outputId = OutputId.BED;
        } else if (params.id.equals(OnlineHmmData.OUTPUT_MODEL_SLEEP)) {
            outputId = OutputId.SLEEP;
        }


        final AlphabetHmmPrior.Builder builder = AlphabetHmmPrior.newBuilder();

        builder.setId(params.id);

        if (outputId != null) {
            builder.setOutputId(outputId);
        }

        builder.setDateCreatedUtc(params.timeCreatedUtc);
        builder.setDateUpdatedUtc(params.timeUpdatedUtc);

        builder.addAllLogDenominator(getDoubleList(params.logDenominator));
        builder.addAllLogObservationModelIds(params.logAlphabetNumerators.keySet());

        for (final String key : params.logAlphabetNumerators.keySet()) {
            final double[][] mtx = params.logAlphabetNumerators.get(key);
            builder.addLogObservationModelNumerator(getProtobufMatrix(mtx));
        }

        builder.setLogStateTransitionNumerator(getProtobufMatrix(params.logTransitionMatrixNumerator));

        return builder.build();
    }

    public byte[] serializeToProtobuf() {

        final AlphabetHmmUserModel.Builder builder = AlphabetHmmUserModel.newBuilder();

        for (final String key : modelsByOutputId.keySet()) {
            final Map<String,OnlineHmmModelParams> paramsMap = modelsByOutputId.get(key);

            for (final Map.Entry<String,OnlineHmmModelParams> params : paramsMap.entrySet()) {
                builder.addModels(protobufFromParams(params.getValue()));
            }
        }

        return builder.build().toByteArray();
    }


    private static String defaultModel = "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";

    public static Optional<OnlineHmmPriors> createDefaultPrior() {
        return createFromProtoBuf(Base64.decodeBase64(defaultModel));
    }
}



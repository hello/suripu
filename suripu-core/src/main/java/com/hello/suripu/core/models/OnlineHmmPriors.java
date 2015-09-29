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
import com.hello.suripu.core.algorithmintegration.MotionTransitionRestriction;
import com.hello.suripu.core.algorithmintegration.OnlineHmm;
import com.hello.suripu.core.algorithmintegration.TransitionRestriction;
import org.apache.commons.codec.binary.Base64;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/18/15.
 */
public class  OnlineHmmPriors {

    public final Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId;

    private OnlineHmmPriors(Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId) {
        this.modelsByOutputId = modelsByOutputId;
    }

    public boolean isEmpty() {
        return modelsByOutputId.isEmpty();
    }

    @Override
    public OnlineHmmPriors clone() {
        final Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId = Maps.newHashMap();

        for (final Map.Entry<String,Map<String,OnlineHmmModelParams>> entry : this.modelsByOutputId.entrySet()) {
            final Map<String,OnlineHmmModelParams> myMap = Maps.newHashMap();

            for (final Map.Entry<String,OnlineHmmModelParams> params : entry.getValue().entrySet()) {
                myMap.put(params.getValue().id, params.getValue().clone());
            }

            modelsByOutputId.put(entry.getKey(),myMap);
        }



        return new OnlineHmmPriors(modelsByOutputId);
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

    public static OnlineHmmPriors createEmpty() {
        final Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId = Maps.newHashMap();

        return new OnlineHmmPriors(modelsByOutputId);
    }

    public static Optional<OnlineHmmModelParams> protobufToParams(final AlphabetHmmPrior protobuf) {

        final boolean hasOutputId = protobuf.hasOutputId();
        final boolean hasId = protobuf.hasId();
        final boolean hasLogANumerator = protobuf.hasLogStateTransitionNumerator();
        final boolean hasLogDenominator = protobuf.getLogDenominatorCount() > 0;
        final boolean hasLogAlphabetNumerator = protobuf.getLogObservationModelNumeratorCount() > 0;
        final boolean hasPi = protobuf.getPiCount() > 0;
        final boolean hasMinStateDuration = protobuf.getMinimumStateDurationsCount() > 0;
        final boolean hasEndState = protobuf.getEndStatesCount() > 0;

        if (! (hasOutputId && hasId && hasLogANumerator && hasLogDenominator && hasLogAlphabetNumerator && hasPi && hasMinStateDuration && hasEndState) ) {
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

        //deal with transition restrictions on a per-model basis
        final List<TransitionRestriction> transitionRestrictions = Lists.newArrayList();

        if (protobuf.hasMotionModelRestriction()) {
            final Optional<MotionTransitionRestriction> restriction = MotionTransitionRestriction.createFromProtobuf(protobuf.getMotionModelRestriction());

            if (restriction.isPresent()) {
                transitionRestrictions.add(restriction.get());
            }
        }

        final Map<String, double[][]> logAlphabetNumerators = Maps.newHashMap();

        for (int i = 0; i < protobuf.getLogObservationModelNumeratorCount(); i++) {
            final double[][] mtx = getMatrix(protobuf.getLogObservationModelNumerator(i));
            final String modelId = protobuf.getLogObservationModelIds(i);

            logAlphabetNumerators.put(modelId, mtx);
        }


        return Optional.of(new OnlineHmmModelParams(logAlphabetNumerators, logStateTransition, logDenominator, pi, endStates, minStateDurations, timeCreated, timeUpdated, protobuf.getId(), protobufEnumToString(protobuf.getOutputId()),transitionRestrictions));

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


            return Optional.of(new OnlineHmmPriors(modelsByOutputId));


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

        if (params.outputId.equals(OnlineHmmData.OUTPUT_MODEL_BED)) {
            outputId = OutputId.BED;
        } else if (params.outputId.equals(OnlineHmmData.OUTPUT_MODEL_SLEEP)) {
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

        for (int i = 0; i < params.pi.length; i++) {
            builder.addPi(params.pi[i]);
        }

        for (int i = 0; i < params.endStates.length; i++) {
            builder.addEndStates(params.endStates[i]);
        }

        for (int i = 0; i < params.minStateDurations.length; i++) {
            builder.addMinimumStateDurations(params.minStateDurations[i]);
        }

        for (final TransitionRestriction restriction : params.transitionRestrictions) {
            final MotionTransitionRestriction castRestriction = (MotionTransitionRestriction)restriction;

            if (castRestriction != null) {
                builder.setMotionModelRestriction(castRestriction.toProtobuf());
            }
        }

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



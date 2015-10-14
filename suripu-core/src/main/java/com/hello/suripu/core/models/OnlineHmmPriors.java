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

    public List<String> getModelIds() {
        final List<String> modelIds = Lists.newArrayList();

        for (final Map<String,OnlineHmmModelParams> maps : modelsByOutputId.values()) {
            for (final OnlineHmmModelParams value : maps.values()) {
                modelIds.add(value.id);
            }
        }

        return modelIds;
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
            for (final OnlineHmmModelParams value : modelsByOutputId.get(key).values()) {
                builder.addModels(protobufFromParams(value));
            }
        }

        return builder.build().toByteArray();
    }


    private static String defaultModel = "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";

    public static Optional<OnlineHmmPriors> createDefaultPrior() {
        return createFromProtoBuf(Base64.decodeBase64(defaultModel));
    }
}



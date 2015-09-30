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



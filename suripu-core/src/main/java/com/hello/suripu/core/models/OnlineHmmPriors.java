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



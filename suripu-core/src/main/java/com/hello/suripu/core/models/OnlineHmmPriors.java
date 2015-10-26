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
import com.hello.suripu.core.algorithmintegration.ModelVotingInfo;
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
    public final Map<String,Map<String,ModelVotingInfo>> votingInfo;
    private OnlineHmmPriors(final Map<String, Map<String,OnlineHmmModelParams>> modelsByOutputId, final Map<String,Map<String,ModelVotingInfo>> votingInfo) {
        this.modelsByOutputId = modelsByOutputId;
        this.votingInfo = votingInfo;
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

        final Map<String, Map<String,ModelVotingInfo>> votingInfo = Maps.newHashMap();

        for (final Map.Entry<String,Map<String,ModelVotingInfo>> entry : this.votingInfo.entrySet()) {
            final Map<String,ModelVotingInfo> myMap = Maps.newHashMap();

            for (final Map.Entry<String,ModelVotingInfo> params : entry.getValue().entrySet()) {
                myMap.put(params.getKey(), params.getValue());
            }

            votingInfo.put(entry.getKey(),myMap);
        }



        return new OnlineHmmPriors(modelsByOutputId,votingInfo);
    }


    public void mergeFrom(final OnlineHmmPriors otherPrior) {
        for (final Map.Entry<String,Map<String,OnlineHmmModelParams>> entry : otherPrior.modelsByOutputId.entrySet()) {

            if (!modelsByOutputId.containsKey(entry.getKey())) {
                modelsByOutputId.put(entry.getKey(),Maps.<String,OnlineHmmModelParams>newHashMap());
            }

            modelsByOutputId.get(entry.getKey()).putAll(entry.getValue());
        }

        for (final Map.Entry<String,Map<String,ModelVotingInfo>> entry : otherPrior.votingInfo.entrySet()) {
            if (!votingInfo.containsKey(entry.getKey())) {
                votingInfo.put(entry.getKey(),Maps.<String,ModelVotingInfo>newHashMap());
            }

            votingInfo.get(entry.getKey()).putAll(entry.getValue());
        }
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
        final Map<String, Map<String,ModelVotingInfo>> votingInfo = Maps.newHashMap();

        return new OnlineHmmPriors(modelsByOutputId,votingInfo);
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

            final Map<String,Map<String,ModelVotingInfo>> modelProbabilities = Maps.newHashMap();

            for (final VotingInfo info : protobuf.getVoteInfoList()) {
                if (!modelProbabilities.containsKey(info.getOutputId())) {
                    modelProbabilities.put(info.getOutputId(),Maps.<String,ModelVotingInfo>newHashMap());
                }

                modelProbabilities.get(info.getOutputId()).put(info.getModelId(),new ModelVotingInfo(info));
            }


            return Optional.of(new OnlineHmmPriors(modelsByOutputId,modelProbabilities));


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

        for (final Map.Entry<String,Map<String,ModelVotingInfo>> entry : votingInfo.entrySet()) {
            for (final Map.Entry<String,ModelVotingInfo> entryForModel : entry.getValue().entrySet()) {
                builder.addVoteInfo(
                        VotingInfo.newBuilder().
                                setModelId(entryForModel.getKey()).
                                setOutputId(entry.getKey()).
                                setProbabilityOfModel(entryForModel.getValue().prob).build());
            }
        }

        return builder.build().toByteArray();
    }
    
}



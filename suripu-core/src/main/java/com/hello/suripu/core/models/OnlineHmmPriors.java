package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.algorithm.hmm.*;
import com.hello.suripu.api.datascience.OnlineHmmProtos.*;
import com.hello.suripu.api.datascience.OnlineHmmProtos.Transition;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/18/15.
 */
public class  OnlineHmmPriors {

    public final Map<String,List<OnlineHmmModelParams>> modelsByOutputId;
    public final List<com.hello.suripu.algorithm.hmm.Transition> forbiddenMotionTransitions;

    private OnlineHmmPriors(Map<String, List<OnlineHmmModelParams>> modelsByOutputId,final List<com.hello.suripu.algorithm.hmm.Transition> forbiddenMotionTransitions) {
        this.modelsByOutputId = modelsByOutputId;
        this.forbiddenMotionTransitions = forbiddenMotionTransitions;
    }

    @Override
    public OnlineHmmPriors clone() {
        final Map<String, List<OnlineHmmModelParams>> modelsByOutputId = Maps.newHashMap();

        for (final String key : modelsByOutputId.keySet()) {
            final List<OnlineHmmModelParams> myList = Lists.newArrayList();
            for (final OnlineHmmModelParams params : modelsByOutputId.get(key)) {
                myList.add(params.clone());
            }

            modelsByOutputId.put(key,myList);
        }

        final List<com.hello.suripu.algorithm.hmm.Transition> myList = Lists.newArrayList();

        for (final com.hello.suripu.algorithm.hmm.Transition transition : forbiddenMotionTransitions) {
            myList.add(transition.clone());
        }

        return new OnlineHmmPriors(modelsByOutputId,myList);
    }


    private static double [][] getMatrix(final RealMatrix protobuf) {
        final double [][] mtx = new double[protobuf.getNumRows()][protobuf.getNumCols()];

        int k = 0;
        for (int j = 0; j < protobuf.getNumRows(); j++) {
            for (int i = 0; i < protobuf.getNumCols(); i++) {
                mtx[j][i] = protobuf.getData(k);
                k++;
            }
        }

        return mtx;
    }

    private static RealMatrix getProtobufMatrix(final double [][] mtx) {
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
                protobuf.getLogObservationModelNumeratorCount() == 0||
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

        final double [][] logStateTransition = getMatrix(protobuf.getLogStateTransitionNumerator());

        final double [] logDenominator = new double[protobuf.getLogDenominatorCount()];
        for (int i = 0; i < protobuf.getLogDenominatorCount(); i++) {
            logDenominator[i] = protobuf.getLogDenominator(i);
        }

        final double [] pi = new double[protobuf.getPiCount()];
        for (int i = 0; i < protobuf.getPiCount(); i++) {
            pi[i] = protobuf.getPi(i);
        }

        final int [] endStates = new int[protobuf.getEndStatesCount()];
        for (int i = 0; i < protobuf.getEndStatesCount(); i++) {
            endStates[i] = protobuf.getEndStates(i);
        }

        final int [] minStateDurations = new int[protobuf.getMinimumStateDurationsCount()];
        for (int i = 0; i < protobuf.getMinimumStateDurationsCount(); i++) {
            minStateDurations[i] = protobuf.getMinimumStateDurations(i);
        }

        final Map<String, double[][]> logAlphabetNumerators = Maps.newHashMap();

        for (int i = 0; i < protobuf.getLogObservationModelNumeratorCount(); i++) {
            final double [][] mtx = getMatrix(protobuf.getLogObservationModelNumerator(i));
            final String modelId = protobuf.getLogObservationModelIds(i);

            logAlphabetNumerators.put(modelId,mtx);
        }

        String outputId = "unknown";
        switch (protobuf.getOutputId()) {

            case SLEEP:
                outputId = OnlineHmmData.OUTPUT_MODEL_SLEEP;
                break;

            case BED:
                outputId = OnlineHmmData.OUTPUT_MODEL_BED;
                break;
        }

        return Optional.of(new OnlineHmmModelParams(logAlphabetNumerators,logStateTransition,logDenominator,pi,endStates,minStateDurations,timeCreated,timeUpdated,protobuf.getId(),outputId));

    }

    public static Optional<OnlineHmmPriors> createFromProtoBuf(final byte [] data) {
        try {
            final AlphabetHmmUserModel protobuf = AlphabetHmmUserModel.parseFrom(data);

            final Map<String,List<OnlineHmmModelParams>> modelsByOutputId = Maps.newHashMap();

            for (final AlphabetHmmPrior prior : protobuf.getModelsList()) {
                final Optional<OnlineHmmModelParams> paramsOptional = protobufToParams(prior);

                if (!paramsOptional.isPresent()) {
                    continue;
                }

                final OnlineHmmModelParams params = paramsOptional.get();

                if (modelsByOutputId.get(params.outputId) == null) {
                    modelsByOutputId.put(params.outputId, Lists.<OnlineHmmModelParams>newArrayList());
                }

                modelsByOutputId.get(params.outputId).add(params);

            }


            List<com.hello.suripu.algorithm.hmm.Transition> transitions = Lists.newArrayList();
            for (final Transition transition : protobuf.getForbiddedenMotionTransitionsList()) {
                transitions.add(new com.hello.suripu.algorithm.hmm.Transition(transition.getFrom(),transition.getTo()));
            }

            return Optional.of(new OnlineHmmPriors(modelsByOutputId,transitions));


        }
        catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return Optional.absent();
        }
    }

    private static List<Double> getDoubleList(final double [] x) {
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
        }
        else if (params.id.equals(OnlineHmmData.OUTPUT_MODEL_SLEEP)) {
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
            final double [][] mtx = params.logAlphabetNumerators.get(key);
            builder.addLogObservationModelNumerator(getProtobufMatrix(mtx));
        }

        builder.setLogStateTransitionNumerator(getProtobufMatrix(params.logTransitionMatrixNumerator));

        return builder.build();
    }

    public byte [] serializeToProtobuf() {

        final AlphabetHmmUserModel.Builder builder = AlphabetHmmUserModel.newBuilder();

        for (final String key : modelsByOutputId.keySet()) {
            final List<OnlineHmmModelParams> paramsList = modelsByOutputId.get(key);

            for (final OnlineHmmModelParams params : paramsList) {
                builder.addModels(protobufFromParams(params));
            }
        }

        return builder.build().toByteArray();
    }
}

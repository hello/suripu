package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.datascience.OnlineHmmProtos.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/18/15.
 */
public class OnlineHmmPriors {
    public static final String OUTPUT_MODEL_BED = "BED";
    public static final String OUTPUT_MODEL_SLEEP = "SLEEP";

    static public class ModelParams  {
        public final Map<String,double [][]> logAlphabetNumerators;
        public final double [][] logTransitionMatrixNumerator;
        public final double [] logDenominator;
        public final long timeCreatedUtc;
        public final long timeUpdatedUtc;
        public final String id;
        public final String outputId;

        public ModelParams(final Map<String, double[][]> logAlphabetNumerators, final double[][] logTransitionMatrixNumerator, final double[] logDenominator,final long timeCreatedUtc, final long timeUpdatedUtc, final String id, final String outputId) {
            this.logAlphabetNumerators = logAlphabetNumerators;
            this.logTransitionMatrixNumerator = logTransitionMatrixNumerator;
            this.logDenominator = logDenominator;
            this.timeCreatedUtc = timeCreatedUtc;
            this.timeUpdatedUtc = timeUpdatedUtc;
            this.id = id;
            this.outputId = outputId;
        }
    }

    public final Map<String,List<ModelParams>> modelsByOutputId;


    private OnlineHmmPriors(Map<String, List<ModelParams>> modelsByOutputId) {
        this.modelsByOutputId = modelsByOutputId;
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

    private static Optional<ModelParams> protobufToParams(final AlphabetHmmPrior protobuf) {

        //things that aren't really optional
        if (!protobuf.hasOutputId() ||
                !protobuf.hasOutputId() ||
                !protobuf.hasLogStateTransitionNumerator() ||
                protobuf.getLogDenominatorCount() == 0 ||
                protobuf.getLogObservationModelNumeratorCount() == 0) {
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


        final Map<String, double[][]> logAlphabetNumerators = Maps.newHashMap();

        for (int i = 0; i < protobuf.getLogObservationModelNumeratorCount(); i++) {
            final double [][] mtx = getMatrix(protobuf.getLogObservationModelNumerator(i));
            final String modelId = protobuf.getLogObservationModelIds(i);

            logAlphabetNumerators.put(modelId,mtx);
        }

        String outputId = "unknown";
        switch (protobuf.getOutputId()) {

            case SLEEP:
                outputId = OUTPUT_MODEL_SLEEP;
                break;

            case BED:
                outputId = OUTPUT_MODEL_BED;
                break;
        }

        return Optional.of(new ModelParams(logAlphabetNumerators,logStateTransition,logDenominator,timeCreated,timeUpdated,protobuf.getId(),outputId));

    }

    public static Optional<OnlineHmmPriors> createFromProtoBuf(final byte [] data) {
        try {
            final AlphabetHmmUserModel protobuf = AlphabetHmmUserModel.parseFrom(data);

            final Map<String,List<ModelParams>> modelsByOutputId = Maps.newHashMap();

            for (final AlphabetHmmPrior prior : protobuf.getModelsList()) {
                final Optional<ModelParams> paramsOptional = protobufToParams(prior);

                if (!paramsOptional.isPresent()) {
                    continue;
                }

                final ModelParams params = paramsOptional.get();

                if (modelsByOutputId.get(params.outputId) == null) {
                    modelsByOutputId.put(params.outputId, Lists.<ModelParams>newArrayList());
                }

                modelsByOutputId.get(params.outputId).add(params);

            }

            return Optional.of(new OnlineHmmPriors(modelsByOutputId));


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

    private static AlphabetHmmPrior priorFromParams(final ModelParams params) {

        OutputId outputId = null;

        if (params.id.equals(OUTPUT_MODEL_BED)) {
            outputId = OutputId.BED;
        }
        else if (params.id.equals(OUTPUT_MODEL_SLEEP)) {
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
            final List<ModelParams> paramsList = modelsByOutputId.get(key);

            for (final ModelParams params : paramsList) {
                builder.addModels(priorFromParams(params));
            }
        }

        return builder.build().toByteArray();
    }
}

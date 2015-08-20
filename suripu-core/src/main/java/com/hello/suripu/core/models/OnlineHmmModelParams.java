package com.hello.suripu.core.models;

import java.util.Map;

/**
 * Created by benjo on 8/19/15.
 */
public class OnlineHmmModelParams {
    public final Map<String,double [][]> logAlphabetNumerators;
    public final double [][] logTransitionMatrixNumerator;
    public final double [] logDenominator;
    public final long timeCreatedUtc;
    public final long timeUpdatedUtc;
    public final String id;
    public final String outputId;

    public OnlineHmmModelParams(final Map<String, double[][]> logAlphabetNumerators, final double[][] logTransitionMatrixNumerator, final double[] logDenominator,final long timeCreatedUtc, final long timeUpdatedUtc, final String id, final String outputId) {
        this.logAlphabetNumerators = logAlphabetNumerators;
        this.logTransitionMatrixNumerator = logTransitionMatrixNumerator;
        this.logDenominator = logDenominator;
        this.timeCreatedUtc = timeCreatedUtc;
        this.timeUpdatedUtc = timeUpdatedUtc;
        this.id = id;
        this.outputId = outputId;
    }

}

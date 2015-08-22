package com.hello.suripu.core.models;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.algorithm.hmm.LogMath;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by benjo on 8/19/15.
 */
public class OnlineHmmModelParams {
    public final Map<String,double [][]> logAlphabetNumerators;
    public final double [][] logTransitionMatrixNumerator;
    public final double [] logDenominator;
    public final double [] pi;
    public final int [] endStates;
    public final int [] minStateDurations;
    public final long timeCreatedUtc;
    public final long timeUpdatedUtc;
    public final String id;
    public final String outputId;

    public OnlineHmmModelParams(final Map<String, double[][]> logAlphabetNumerators, final double[][] logTransitionMatrixNumerator, final double[] logDenominator,final double [] pi, final int [] endStates,final int [] minStateDurations, final long timeCreatedUtc, final long timeUpdatedUtc, final String id, final String outputId) {
        this.logAlphabetNumerators = logAlphabetNumerators;
        this.logTransitionMatrixNumerator = logTransitionMatrixNumerator;
        this.logDenominator = logDenominator;
        this.pi = pi;
        this.endStates = endStates;
        this.minStateDurations = minStateDurations;
        this.timeCreatedUtc = timeCreatedUtc;
        this.timeUpdatedUtc = timeUpdatedUtc;
        this.id = id;
        this.outputId = outputId;
    }

    public double [][] clone2d (final double [][] x) {
        final double [][] y = new double[x.length][];
        for(int i = 0; i < x.length; i++) {
            y[i] = x[i].clone();
        }

        return y;
    }

    public OnlineHmmModelParams clone() {
        final Map<String,double [][]> logNumerator = Maps.newHashMap();

        for (final String key : logAlphabetNumerators.keySet()) {
            logNumerator.put(key,clone2d(logAlphabetNumerators.get(key)));
        }


        return new OnlineHmmModelParams(logNumerator,clone2d(logTransitionMatrixNumerator),logDenominator.clone(),pi.clone(),endStates.clone(),minStateDurations.clone(),timeCreatedUtc,timeUpdatedUtc,id,outputId);
    }



}

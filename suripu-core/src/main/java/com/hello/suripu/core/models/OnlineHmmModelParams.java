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


    /*
    public OnlineHmmModelParams update(final OnlineHmmModelParams delta, final long updateTimeUtc, final double newPriorWeight) {
        //update numerator and denominator
        final double [][] logTransitionMatrixNumerator = LogMath.elnAddMatrix(this.logTransitionMatrixNumerator, delta.logTransitionMatrixNumerator);
        final double [] logDenominator = LogMath.elnAddVector(this.logDenominator,delta.logDenominator);

        final Map<String,double [][]> logNumerator = Maps.newHashMap();

        final Set<String> thisKeys = Sets.newHashSet(this.logAlphabetNumerators.keySet());
        final Set<String> deltaKeys = Sets.newHashSet(this.logAlphabetNumerators.keySet());

        //set differences
        thisKeys.removeAll(deltaKeys);
        deltaKeys.removeAll(this.logAlphabetNumerators.keySet());

        //TODO log if there are any differences


        for (final String key : this.logAlphabetNumerators.keySet()) {
            final double [][] thislogAlphabetNumerator = this.logAlphabetNumerators.get(key);
            final double [][] deltalogAlphabetNumerator = delta.logAlphabetNumerators.get(key);

            //if there is no match in the delta, then just place the original
            if (deltalogAlphabetNumerator == null) {
                logNumerator.put(key,thislogAlphabetNumerator);
                continue;
            }

            logNumerator.put(key,LogMath.elnAddMatrix(thislogAlphabetNumerator,deltalogAlphabetNumerator));
        }



        return new OnlineHmmModelParams(logNumerator,logTransitionMatrixNumerator,logDenominator,pi,endStates,minStateDurations,updateTimeUtc,updateTimeUtc,id,outputId);

    }
    */

}

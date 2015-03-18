package com.hello.suripu.algorithm.hmm;

import com.google.common.collect.ImmutableList;

/**
 * Created by benjo on 3/16/15.
 */
public class HmmDecodedResult {
    public final ImmutableList<Integer> bestPath;
    public final double bic; //Bayesian information criterion
    public final double aic; //Akaike information criterion
    public final double pathCost; //

    public HmmDecodedResult(final ImmutableList<Integer> bestPath, double bic, double aic, double pathCost) {
        this.bestPath = bestPath;
        this.bic = bic;
        this.aic = aic;
        this.pathCost = pathCost;
    }
}
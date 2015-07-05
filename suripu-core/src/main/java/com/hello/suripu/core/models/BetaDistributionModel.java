package com.hello.suripu.core.models;

/**
 * Created by benjo on 7/5/15.
 */
public class BetaDistributionModel {

    //intentionally mutable
    public Double alpha;
    public Double beta;

    public BetaDistributionModel(final Double alpha, final Double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }
}

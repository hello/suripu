package com.hello.suripu.algorithm.bayes;

/**
 * Created by benjo on 6/15/15.
 */
public class BetaDistribution {
    Double alpha;
    Double beta;

    public BetaDistribution(final Double alpha, final Double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    public Double getExpectation() {
        return alpha / (alpha + beta);
    }

    public void updateWithInference(final Double probOfSuccess) {
        updateWithInference(probOfSuccess,1);
    }

    public void updateWithInference(final Double probOfSuccess, Integer numObs) {
        alpha += probOfSuccess * numObs.doubleValue();
        beta += probOfSuccess * (1.0 - numObs.doubleValue());

    }

}

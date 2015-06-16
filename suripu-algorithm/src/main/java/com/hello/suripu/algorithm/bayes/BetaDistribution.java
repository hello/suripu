package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.Lists;

import java.util.List;

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

    public static List<BetaDistribution> createBinaryComplementaryBetaDistributions(double p,int numMeasurementsInPrior) {
        List<BetaDistribution> dists = Lists.newArrayList();

        if (numMeasurementsInPrior < 1) {
            numMeasurementsInPrior = 1;
        }

        if (p < 0.0) {
            p = 0.0;
        }

        if (p > 1.0) {
            p = 1.0;
        }

        final double theta1 = p * numMeasurementsInPrior ;
        final double theta2 = (1.0 - p) * numMeasurementsInPrior;

        dists.add(new BetaDistribution(theta1,theta2));
        dists.add(new BetaDistribution(theta2,theta1));

        return dists;
    }

}

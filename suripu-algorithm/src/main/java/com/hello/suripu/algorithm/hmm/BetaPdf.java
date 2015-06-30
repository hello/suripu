package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.distribution.BetaDistribution;

/**
 * Created by benjo on 6/29/15.
 */
public class BetaPdf implements HmmPdfInterface {

    final int obsNum;
    final BetaDistribution betaDistribution;

    public BetaPdf(double alpha, double beta, int obsNum) {
        this.betaDistribution = new BetaDistribution(alpha,beta);
        this.obsNum = obsNum;
    }


    @Override
    public double[] getLogLikelihood(double[][] measurements) {

        final double [] x = measurements[this.obsNum];
        final double [] y = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            double m = x[i];

            if (m > 1.0) {
                m = 1.0;
            }

            if (m < 0.0) {
                m = 0.0;
            }

            double val = this.betaDistribution.density(m);

            if (val < MIN_LIKELIHOOD) {
                val = MIN_LIKELIHOOD;
            }

            y[i] =  Math.log(val);
        }

        return y;
    }

    @Override
    public int getNumFreeParams() {
        return 2;
    }
}

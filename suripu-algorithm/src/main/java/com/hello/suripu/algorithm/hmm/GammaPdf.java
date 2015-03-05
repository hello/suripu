package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.distribution.GammaDistribution;

/**
 * Created by benjo on 3/4/15.
 */
public class GammaPdf implements HmmPdfInterface {
    private  final int measNum;
    private final GammaDistribution gammaDistribution;


    public GammaPdf(final double mean, final double stdDev, final int measNum) {
        this.measNum = measNum;

        // k*theta = mean
        // k*theta^2  = variance
        //
        // k = mean / theta
        // mean / theta * theta^2 = mean * theta = variance
        // theta = variance / mean
        // k = mean / (variance / mean) = mean*mean / variance

        final double variance = stdDev*stdDev;
        final double theta  = variance / mean;
        final double k = mean*mean / variance;

        this.gammaDistribution = new GammaDistribution(k,theta);
    }
    @Override
    public double[] getLikelihood(double[][] measurements) {
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[measNum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function
            result[i] = gammaDistribution.density(col[i]);
        }

        return result;
    }
}

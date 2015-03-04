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

        // alpha/beta = mean
        // alpha/beta^2  = variance
        //
        // alpha = beta * mean
        // beta * mean / beta^2 = variance
        // mean / beta = variance
        // beta = mean / variance
        final double variance = stdDev*stdDev;
        final double beta = mean / variance;
        final double alpha = beta * mean;


        this.gammaDistribution = new GammaDistribution(alpha,beta);
    }
    @Override
    public double[] getLikelihood(double[][] measurements) {
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[measNum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function

            result[i] = gammaDistribution.probability(col[i]);
        }

        return result;
    }
}

package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.distribution.GammaDistribution;

/**
 * Created by benjo on 3/4/15.
 */
public class GammaPdf implements HmmPdfInterface {
    private  final int measNum;
    private final GammaDistribution gammaDistribution;
    private final static double MIN_INPUT_VALUE = 1e-1;
    private final double weight;

    public GammaPdf(final double mean, final double stdDev, final int measNum, final double weight) {
        this.measNum = measNum;
        this.weight = weight;

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
    public double[] getLogLikelihood(double[][] measurements) {
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[measNum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function
            double inputValue = col[i];
            if (inputValue < MIN_INPUT_VALUE) {
                inputValue = MIN_INPUT_VALUE;
            }

            double pdfEval = gammaDistribution.density(inputValue);

            if (pdfEval < MIN_LIKELIHOOD) {
                pdfEval = MIN_LIKELIHOOD;
            }

            result[i] = weight * Math.log(pdfEval);

            if (Double.isNaN(result[i])) {
                result[i] = Double.NEGATIVE_INFINITY;
            }
        }

        return result;
    }
}

package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Created by benjo on 5/22/15.
 */
public class OneDimensionalGaussianPdf implements HmmPdfInterface {
    final int measNum;
    final double weight;
    final  NormalDistribution normalDistribution;

    public OneDimensionalGaussianPdf(final double mean, final double stddev, final int measNum, final double weight) {

        this.measNum = measNum;
        this.weight = weight;

        this.normalDistribution = new NormalDistribution(mean,stddev);

    }

    @Override
    public double[] getLogLikelihood(double[][] measurements) {
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[measNum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function


            double pdfEval = normalDistribution.density(col[i]);

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

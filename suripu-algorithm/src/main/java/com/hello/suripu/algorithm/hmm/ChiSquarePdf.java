package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

/**
 * Created by benjo on 6/21/15.
 */
public class ChiSquarePdf implements HmmPdfInterface {
    final int measNum;
    final double mean;
    final ChiSquaredDistribution chiSquaredDistribution;
    private final static double MIN_INPUT_VALUE = 1e-1;

    public ChiSquarePdf(final double mean, final int measNum) {
        this.chiSquaredDistribution = new ChiSquaredDistribution(1);
        this.measNum = measNum;
        this.mean = mean;


    }

    @Override
    public double[] getLogLikelihood(double[][] measurements) {
        final double scale = 1.0 / Math.sqrt(2.0 * mean);
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[measNum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function

            double inputValue = col[i];

            if (inputValue < MIN_INPUT_VALUE) {
                inputValue = MIN_INPUT_VALUE;
            }

            double pdfEval = this.chiSquaredDistribution.density(inputValue * scale) ;

            if (pdfEval < MIN_LIKELIHOOD) {
                pdfEval = MIN_LIKELIHOOD;
            }

            result[i] = Math.log(pdfEval);

        }

        return result;
    }

    @Override
    public int getNumFreeParams() {
        return 1;
    }
}

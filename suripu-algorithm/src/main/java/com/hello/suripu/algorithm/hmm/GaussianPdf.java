package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.distribution.NormalDistribution;


public class GaussianPdf implements HmmPdfInterface {
    private  final int measNum;
    private final NormalDistribution normalDistribution;

    public GaussianPdf(final double mean, final double stdDev, final int measNum) {
        this.measNum = measNum;
        this.normalDistribution = new NormalDistribution(mean,stdDev);
    }
    @Override
    public double[] getLogLikelihood(double[][] measurements) {
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[measNum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function

            double pdfEval = normalDistribution.density(col[i]);


            result[i] = Math.log(pdfEval);

        }

        return result;
    }

    @Override
    public int getNumFreeParams() {
        return 2;
    }
}

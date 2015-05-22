package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.distribution.PoissonDistribution;

/**
 * Created by benjo on 2/21/15.
 */
public class PoissonPdf implements  HmmPdfInterface {
    int measnum;
    PoissonDistribution poisson;
    double weight;

    public PoissonPdf(final double mean,final int measnum,final double weight) {
        this.measnum = measnum;
        this.poisson = new PoissonDistribution(mean);
        this.weight = weight;
    }

    @Override
    public double [] getLogLikelihood(final double [][] measurements) {
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[measnum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function
            double pmfEval = poisson.probability((int)col[i]);

            if (pmfEval < MIN_LIKELIHOOD) {
                pmfEval = MIN_LIKELIHOOD;
            }

            result[i] = weight * Math.log(pmfEval);

            if (Double.isNaN(result[i])) {
                result[i] = Double.NEGATIVE_INFINITY;
            }
        }

        return result;
    }
}

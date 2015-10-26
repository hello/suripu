package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by benjo on 2/21/15.
 */
public class PoissonPdf implements  HmmPdfInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoissonPdf.class);

    int _measnum;
    PoissonDistribution _poisson;

    public PoissonPdf(double mean, int measnum) {
        _measnum = measnum;
        _poisson = new PoissonDistribution(mean);
    }

    @Override
    public double [] getLogLikelihood(final double [][] measurements) {
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[_measnum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function
            double pmfEval = _poisson.probability((int)col[i]);

            result[i] = Math.log(pmfEval);
        }

        return result;
    }

    @Override
    public int getNumFreeParams() {
        return 1;
    }
}

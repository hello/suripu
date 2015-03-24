package com.hello.suripu.algorithm.hmm;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by benjo on 2/21/15.
 */
public interface HmmPdfInterface {
    static public final double MIN_LIKELIHOOD = 1e-15;

    public double [] getLogLikelihood(final double [][] measurements);
}

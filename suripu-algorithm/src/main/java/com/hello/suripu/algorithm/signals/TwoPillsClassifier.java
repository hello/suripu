package com.hello.suripu.algorithm.signals;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by benjo on 2/21/15.
 */
public class TwoPillsClassifier {

    public static double [][] clone2D(double [][] x) {
        double [][] x2 = new double [x.length][] ;

        for (int i = 0; i < x.length; i++) {
            x2[i] = x[i].clone();
        }

        return x2;
    }

    public static double getMean(final double [] x) {
        double n = x.length;
        double mean = 0.0;

        for (int i = 0; i < x.length; i++) {
            mean += x[i];
        }

        mean /= n;

        return mean;
    }

    public static void separateSignals (final double [][] data, int numFeaturesPerAxis)  throws MathArithmeticException {

        final int nFeats = data.length;
        double [][] x = clone2D(data);


        //remove mean
        for (int i = 0; i < nFeats; i++ ) {
            final double theMean = getMean(x[i]);
            double [] row = x[i];
            for (int j = 0; j < row.length; j++) {
                row[j] -= theMean;
            }
        }

        //compute covariance
        RealMatrix nomean = MatrixUtils.createRealMatrix(x);
        RealMatrix P = nomean.multiply(nomean.transpose());

        double [] sqrtDiags = new double[nFeats];
        for (int i = 0; i < nFeats; i++ ) {
            sqrtDiags[i] = Math.sqrt(P.getEntry(i,i));
        }

        for (int i = 0; i < nFeats; i++ ) {
            if (Double.isNaN(sqrtDiags[i]) || sqrtDiags[i] < 1e-6) {
                //FUCK
                throw new MathArithmeticException();
            }
        }

        //turn covariance into correlation matrix
        for (int j = 0; j < nFeats; j++) {
            for (int i = 0; i  < nFeats; i++) {
                P.getData()[j][i] /= (sqrtDiags[j] * sqrtDiags[i]);
            }
        }

        EigenDecomposition decomposition = new EigenDecomposition(P);

        RealVector magnitudevec = decomposition.getEigenvector(0);
        RealVector diffvec = decomposition.getEigenvector(1);

        int foo = 3;
        foo++;




            //normalzie by variance

    }

}

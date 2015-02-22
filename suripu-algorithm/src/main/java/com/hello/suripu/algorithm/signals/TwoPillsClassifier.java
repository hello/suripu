package com.hello.suripu.algorithm.signals;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Arrays;

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

    /*  classifyPillOwnership
     *
     *  What does this do? Returns classification of which person/pill the data actually belonged to.
     *
     *  class = 1 ===> my pill
     *  class = -1 ==> other pill
     *
     *  -There are two sources and two measurements, as time series (person1, pill1_vecs(t), person2, pill2_vecs(t) )
     *  -person 1 moves and it shows up on pill1, and to a lesser extent pill2
     *  -person 2 moves and it shows up on pill2, and to a lesser extend pill1.
     *  -I can go and compute the covariance between pill1 and pill2 data.  What does this tell us?
     *
     *   It tells us how correlated pill1 and pill2 are, and on what feature (magnitude, duration, kickoff counts, etc.).
     *
     *   Great, now what?
     *
     *   If I do an Eigen value decomposition on this covariance matrix, I will find find the independent combination of features
     *   that is present in the data, ranked by how much variance/"energy" of the data is in that dimension.  And, I would hope that this
     *   corresponds to something meaningful.
     *
     *   Fooling around with this, we consistently see that the first most energy combination (the Eigen vector) is
     *   a1 * x1 + a2*x2 + a3*x3.... where ai > 0.3
     *   this is basically saying "yeah, the most energy is in the data amplitude"  Well that's comforting
     *
     *   Almost always the next most energetic Eigen vector is
     *   a1*x2 + a2*x2 + a3*x3...  a1,a2,a3 > 0.3, and a3,a4,a5 < -0.3
     *
     *   This is saying that the difference between pill1 feats and pill2 feats is an important feature.  And, it gave us the optimal
     *   weights to calculate this feature.
     *
     *   So, we check to make sure the second eigenvectors what we expect.  If it's not, we just return the original data.
     *   Then, we transform the data into the direction of this eigenvector, and say if it's really greater than zero, it came from pill1
     *   and if it's really less than 0 it came from pill2.
     *
     *   In our case, if it came from pill2, we remove the data point.
     *
     *
     *
     */
    private static double EIGEN_MODE_THRESHOLD = 0.5;
    private static double NORMALIZED_FEATURE_THRESHOLD = 0.25;

    public static int [] classifyPillOwnership(final double[][] data, int numFeaturesPerAxis)  {

        final int nFeats = data.length;
        double [][] x = clone2D(data);

        int [] classes = new int[data[0].length];
        Arrays.fill(classes, 1);

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
                return classes;
            }
        }

        //normalize the data by the std devs
        for (int i = 0; i < nFeats; i++ ) {
            final double theMean = getMean(x[i]);
            double [] row = x[i];
            double d = sqrtDiags[i];
            for (int j = 0; j < row.length; j++) {
                row[j] /= d;
            }
        }

        RealMatrix normalizedByStdDev = MatrixUtils.createRealMatrix(x);

        //turn covariance into correlation matrix
        RealMatrix Pnormalized = normalizedByStdDev.multiply(normalizedByStdDev.transpose());


        EigenDecomposition decomposition = new EigenDecomposition(Pnormalized);

        RealVector magnitudevec = decomposition.getEigenvector(0);
        RealVector diffvec = decomposition.getEigenvector(1);

        //so I am expecting mode 1, the "diffvec" to look like [0.4,0.4,0.4,-0.4,-0.4,-0.4] or something like this
        //these should be the optimal weights to decide who the data belongs to (person 1 or person 2)
        double componentSum = 0.0;

        for (int i = 0; i < numFeaturesPerAxis; i++) {
            componentSum += diffvec.getEntry(i);
        }

        //if this condition is not met, it might mean the mode we're looking for isn't here.
        if (Math.abs(componentSum) > EIGEN_MODE_THRESHOLD) {
            if (componentSum < 0.0) {
                diffvec.mapMultiplyToSelf(-1.0);
            }

            RealMatrix eigvec = MatrixUtils.createRowRealMatrix(diffvec.toArray());


            RealMatrix feats = eigvec.multiply(normalizedByStdDev);

            double [] projectedData = feats.getData()[0];

            for (int i = 0; i < projectedData.length; i++) {
                if (projectedData[i] < -NORMALIZED_FEATURE_THRESHOLD) {
                    classes[i] = -1; //came from the other pill
                }
            }

        }




        return classes;
    }

}

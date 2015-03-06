package com.hello.suripu.algorithm.signals;

import com.google.common.base.Optional;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Arrays;

/**
 * Created by benjo on 2/21/15.
 */
public class TwoPillsClassifier {

    private final static double SIMILARITY_THRESHOLD = 0.707;
    private final static double LOG_RATIO_THRESHOLD = 0.2;


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


    public static int CORRELATION_WINDOW_SIZE = 5; //pick an odd number please


    private static class DotProdResut {
        public DotProdResut(final double cosAngle, final double v1mag,final double v2mag) {
            this.cosAngle = cosAngle;
            this.v1mag = v1mag;
            this.v2mag = v2mag;
        }

        public final double cosAngle;
        public final double v1mag;
        public final double v2mag;
    }

    private static Optional<Double> normalize(final double [] v) {
        final int n = v.length;

        double sumsquare = 0.0;
        for (int i = 0; i < n; i++) {
            sumsquare += v[i]*v[i];
        }

        final double norm = Math.sqrt(sumsquare);

        if (norm < 1e-8) {
            return Optional.absent();
        }


        for (int i = 0; i < n; i++) {
            v[i] /= norm;
        }

        return Optional.of(norm);


    }

    private static Optional<DotProdResut> dotProd(final double [] v1, final double [] v2) {
        final int n = v1.length;

        final double [] v1copy = v1.clone();
        final double [] v2copy = v2.clone();

        final Optional<Double> v1mag = normalize(v1copy);

        final Optional<Double> v2mag  = normalize(v2copy);

        if (!v1mag.isPresent() || !v2mag.isPresent()) {
            return Optional.absent();
        }

        double cosAngle = 0.0;
        for (int i = 0; i < n; i++) {
            cosAngle += v1copy[i] * v2copy[i];
        }

        return Optional.of(new DotProdResut(cosAngle,v1mag.get(),v2mag.get()));
    }



    public static int [] classifyPillOwnershipByMovingSimilarity(final double[][] xAppendedInTime) {
        final int nFeats = xAppendedInTime.length;
        double [][] dataCopy = clone2D(xAppendedInTime);
        final int numberDataPoints = xAppendedInTime[0].length/2;

        //default == every data point is mine
        final int [] classes = new int[numberDataPoints];
        Arrays.fill(classes, 1);

        //this data is normalized by variance too.
        final Optional<RealMatrix> uncorrelatedDataOptional = getUncorrelatedDataPoints(xAppendedInTime);

        //if the decorrelation didn't go well, return the default
        if (!uncorrelatedDataOptional.isPresent()) {
            return classes;
        }

        final RealMatrix uncorrelatedData = uncorrelatedDataOptional.get();

        //go through and compute dot products, normalized, in a sliding window
        //closer to 1.0 it is, the more similar the vectors are
        //if the vectors are similar, we then decide which person generated them.
        //we do this by comparing the vector magnitudes.  If one vector is significantly larger
        //then then the larger vector is the winner.
        //THERFORE we get 3 classes, mine, yours, and unsure.
        for (int i = 0; i < numberDataPoints; i++) {
            final double [] v1 = uncorrelatedData.getColumn(i);
            final double [] v2 = uncorrelatedData.getColumn(i + numberDataPoints);

            Optional<DotProdResut> dotProdOptional = dotProd(v1,v2);

            if (!dotProdOptional.isPresent()) {
                continue;
            }

            final DotProdResut dotProd = dotProdOptional.get();

            if (dotProd.cosAngle > SIMILARITY_THRESHOLD) {
                final double logRatio = Math.log( (dotProd.v1mag + 1e-15) / (dotProd.v2mag + 1e-15)) ;

                if (logRatio > LOG_RATIO_THRESHOLD) {
                    classes[i] = 1; //v1 mag is much larger, so it's mine
                }
                else if (logRatio < -LOG_RATIO_THRESHOLD) { //v2mag is much larger, so not mine
                    classes[i] = -1;
                }
                else {
                    classes[i] = 0; //don't know
                }

            }
        }





        return classes;

    }

    static private void copySection(final double [][] orig, final double [][] section, final int startIdx, final int stopIdx) {


        for (int j = startIdx; j < stopIdx; j++) {
            final double [] rowOrig =  orig[j];
            final double [] rowSection =  section[j];

            int k = 0;
            for (int i = 0; i < orig.length; i++) {
                rowSection[k] = rowOrig[k];
                k++;
            }
        }

    }


    /*
    *  step 1) Get covariance matrix, normalize by sqrt of diagonals to get correlation matrix
    *  step 2) Get cholesky factor of correlation matrix, and invert this.  Call this the "decorrelation transform matrix"
    *  step 3) multiply data by the decorrelation transform matrix, and now you have magically decorrelated data
    *
    */
    static public Optional<RealMatrix> getUncorrelatedDataPoints(final double [][] data) {

        final int nFeats = data.length;
        final double [][] dataCopy = clone2D(data);
        final int numberOfDataPoints = data[0].length;

        //remove mean
        for (int i = 0; i < nFeats; i++ ) {
            final double theMean = getMean(dataCopy[i]);
            double [] row = dataCopy[i];
            for (int j = 0; j < row.length; j++) {
                row[j] -= theMean;
            }
        }


        //compute covariance
        final RealMatrix noMean = MatrixUtils.createRealMatrix(dataCopy);
        final RealMatrix noMeanSquared = noMean.multiply(noMean.transpose());

        final RealMatrix covariance = noMeanSquared.scalarMultiply(1.0 / (double)numberOfDataPoints);

        final double [] squareRootMatrixDiagonals = new double[nFeats];
        for (int i = 0; i < nFeats; i++ ) {
            squareRootMatrixDiagonals[i] = Math.sqrt(covariance.getEntry(i, i));
        }

        for (int i = 0; i < nFeats; i++ ) {
            if (Double.isNaN(squareRootMatrixDiagonals[i]) || squareRootMatrixDiagonals[i] < 1e-6) {
                //FUCK
                return Optional.absent();
            }
        }

        /*
        //normalize the data by the std devs
        for (int i = 0; i < nFeats; i++ ) {
            final double [] row = dataCopy[i];
            final double d = squareRootMatrixDiagonals[i];
            for (int j = 0; j < row.length; j++) {
                row[j] /= d;
            }
        }

        final RealMatrix normalizedByStdDevAndNoMean = MatrixUtils.createRealMatrix(dataCopy);

        //turn covariance into correlation matrix
       // final RealMatrix correlationMatrix = normalizedByStdDevAndNoMean.multiply(normalizedByStdDevAndNoMean.transpose());
*/

        final CholeskyDecomposition choleskyDecomposition = new CholeskyDecomposition(covariance);

        final LUDecomposition inverter = new LUDecomposition(choleskyDecomposition.getL());

        final RealMatrix decorrelated = inverter.getSolver().solve(noMean);


        return Optional.of(decorrelated);


    }

   

}

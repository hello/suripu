package com.hello.suripu.algorithm.hmm;

import com.hello.suripu.algorithm.core.AlgorithmException;

/**
 * Created by benjo on 8/17/15.
 */
public class LogMath {

    public static double LOGZERO = Double.NEGATIVE_INFINITY;
    private static double MIN_NUMBER = Double.MIN_VALUE * 2;

    private static void algorithmAssert(boolean condition,final String assertFailureMessage) {
        if (!condition) {
            throw new AlgorithmException(assertFailureMessage);
        }
    }

    public static double eln(final double x) {
        //if x is zero
        if (x <= MIN_NUMBER) {
            return LOGZERO;
        }
        else {
            return Math.log(x);
        }
    }

    public static double eexp(final double x) {
        if (x == LOGZERO) {
            return 0.0;
        }
        else {
            return Math.exp(x);
        }
    }

    public static double elnproduct(final double logx, final double logy) {
        if (logx == LOGZERO || logy == LOGZERO) {
            return LOGZERO;
        }
        else {
            return logx + logy;
        }
    }

    public static double elnsum(final double logx, final double logy) {
        if (logx == LOGZERO && logy == LOGZERO) {
            return LOGZERO;
        }
        else if (logx == LOGZERO) {
            return logy;
        }
        else if (logy == LOGZERO) {
            return logx;
        }
        else {
            if (logx > logy) {
                return logx + eln(1.0 + eexp(logy - logx));
            }
            else {
                return logy + eln(1.0 + eexp(logx - logy));
            }
        }

    }

    public static double [][] elnAddMatrix(final double [][] logx, final double [][] logy) {
        assert (logy.length == logx.length);
        assert (logy.length > 0);
        assert (logy[0].length == logx[0].length);


        final int m = logy.length;
        final int n = logy[0].length;

        final double[][] logz = new double[m][n];

        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                logz[j][i] = elnsum(logx[j][i], logy[j][i]);
            }
        }

        return logz;
    }

    public static double [] elnAddVector(final double [] logx, final double [] logy) {
        assert (logy.length == logx.length);
        assert (logy.length > 0);

        final int m = logy.length;

        final double[] logz = new double[m];

        for (int j = 0; j < m; j++) {
            logz[j] = elnsum(logx[j], logy[j]);
        }

        return logz;
    }

    public static double [][] elnProductMatrix(final double [][] logx, final double [][] logy) {
        algorithmAssert (logy.length == logx.length,"number of rows do not match");
        algorithmAssert (logy.length > 0,"no rows in matrix");
        algorithmAssert (logy[0].length == logx[0].length,"number of columns do not match");


        final int m = logy.length;
        final int n = logy[0].length;

        final double[][] logz = new double[m][n];

        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                logz[j][i] = elnproduct(logx[j][i], logy[j][i]);
            }
        }

        return logz;
    }

    public static double [][] elnMatrixScalarProduct(final double [][] logx, final double logy) {
        algorithmAssert (logx.length > 0,"no rows in matrix");


        final int m = logx.length;
        final int n = logx[0].length;

        final double[][] logz = new double[m][n];

        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                logz[j][i] = elnproduct(logx[j][i], logy);
            }
        }

        return logz;
    }

    public static double [] elnVectorScalarProduct(final double [] logx, final double logy) {
        algorithmAssert (logx.length > 0,"no rows in matrix");

        final int m = logx.length;

        final double[] logz = new double[m];

        for (int j = 0; j < m; j++) {
            logz[j] = elnproduct(logx[j], logy);
        }

        return logz;
    }


}

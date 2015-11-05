package com.hello.suripu.algorithm.hmm;

import junit.framework.TestCase;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.junit.Test;

/**
 * Created by benjo on 10/23/15.
 */
public class PdfTest {

    @Test
    public void testPoissonPdf() {
        final PoissonPdf poissonPdf1 = new PoissonPdf(1.0,0);
        final PoissonPdf poissonPdf2 = new PoissonPdf(2.0,0);
        final PoissonPdf poissonPdf3 = new PoissonPdf(20.0,0);


        final double [] ref1 = { -1.        ,   -1.        ,   -1.69314718,   -5.78749174,
                -16.10441257, -364.73937556,          Double.NEGATIVE_INFINITY,          Double.NEGATIVE_INFINITY};

        final double [] ref2 = { -2.        ,   -1.30685282,   -1.30685282,   -3.32175584,
                -10.17294077, -296.4246575 ,          Double.NEGATIVE_INFINITY,          Double.NEGATIVE_INFINITY};

        final double [] ref3 = {-20.        , -17.00426773, -14.70168263,  -9.80883038,
                -5.14708984, -84.1661482 ,         Double.NEGATIVE_INFINITY,         Double.NEGATIVE_INFINITY};

        final double [][] x = {{0.0,1.0,2.0,5.0,10.0,100.0,1000.0,1e6}};

        final double [] y1 =  poissonPdf1.getLogLikelihood(x);
        final double [] y2 = poissonPdf2.getLogLikelihood(x);
        final double [] y3 = poissonPdf3.getLogLikelihood(x);

        for (int i = 0; i < y1.length; i++) {
            TestCase.assertEquals(ref1[i],y1[i],1e-5);
            TestCase.assertEquals(ref2[i],y2[i],1e-5);
            TestCase.assertEquals(ref3[i],y3[i],1e-5);
        }



    }

    @Test
    public void testGammaPdf() {
        final double [][] x = {{0.0,1.0,2.0,5.0,10.0,100.0,1000.0,1e6}};

        final double [] ref1 = {   Double.NEGATIVE_INFINITY,  -0.95012756,  -1.88081284,  -4.78918377,
                -9.71986905, -99.48961054,         Double.NEGATIVE_INFINITY,         Double.NEGATIVE_INFINITY};

        final double [] ref2 = { Double.NEGATIVE_INFINITY,   -1.27197611,   -1.65720685,   -2.92921414,
                -5.13262669,  -45.81145909, -454.67210967,          Double.NEGATIVE_INFINITY};

        final GammaDistribution gammaDistribution1 = new GammaDistribution(1.1,1.0);

        for (int i = 0; i < x[0].length;i ++) {
            final double y1 = LogMath.eln(gammaDistribution1.density(x[0][i]))   ;
            TestCase.assertEquals(ref1[i],y1,1e-5);
        }

        final GammaDistribution gammaDistribution2 = new GammaDistribution(1.1,2.2);

        for (int i = 0; i < x[0].length;i ++) {
            final double y2 = LogMath.eln(gammaDistribution2.density(x[0][i]))   ;
            TestCase.assertEquals(ref2[i],y2,1e-5);
        }

        /*

        var / mean = theta
        mean^2 / var = k

        mean = k*theta
        var = k*theta*theta

         */

/*
        final GammaPdf gammaPdf = new GammaPdf(1.1*2.2,Math.sqrt(1.1*2.2*2.2),0);

        final double [] y = gammaPdf.getLogLikelihood(x);

        for (int i = 0; i < x[0].length; i++) {
            TestCase.assertEquals(ref2[i],y[i],1e-5);
        }
*/
    }

}

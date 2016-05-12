package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.interpretation.EventIndices;
import com.hello.suripu.algorithm.interpretation.SleepProbabilityInterpreterWithSearch;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by benjo on 3/24/16.
 */

public class SleepProbabilityInterpreterTest extends SleepProbabilityInterpreterWithSearch {

    @Test
    public void testNoDataFail() {
        final double [] pnone = new double [0];
        final double [] p = new double [961];
        Arrays.fill(p,0.001);

        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(pnone,pnone,pnone,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(pnone,pnone,p,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(pnone,p,pnone,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(pnone,p,p,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(p,pnone,pnone,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(p,pnone,p,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(p,p,pnone,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(p,p,p,pnone).isPresent());

    }

    @Test
    public void testTypicalCase() {
        final double [] p = new double [961];
        final double [] m = new double [961];
        final double [] m2 = new double [961];
        final double [] l = new double [961];


        for (int t=200; t < 500; t++) {
            p[t] = 0.95;
            m[t] = 1.0;
            m2[t] = 2.0;
        }

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m2,l);

        TestCase.assertTrue(indicesOptional.isPresent());

        final EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(indices.iInBed,200,10);
        TestCase.assertEquals(indices.iSleep,200,10);
        TestCase.assertEquals(indices.iWake,500,10);
        TestCase.assertEquals(indices.iOutOfBed,500,10);

    }

    @Test
    public void testCaseWithMotionClustering() {
        final double [] p = new double [961];
        final double [] m = new double [961];
        final double [] l = new double [961];

        m[165] = 1;
        m[169] = 20;
        m[175] = 5;
        m[180] = 5.0;
        m[185] = 8.0;
        m[190] = 6.0;

        m[510] = 3.0;
        m[511] = 3.0;
        m[512] = 3.0;
        m[520] = 20.0;

        for (int t=200; t < 500; t++) {
            p[t] = 0.95;
        }

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m,l);

        TestCase.assertTrue(indicesOptional.isPresent());

        final EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(169,indices.iInBed,2);
        TestCase.assertEquals(indices.iSleep,200,10);
        TestCase.assertEquals(indices.iWake,500,10);
        TestCase.assertEquals(520,indices.iOutOfBed,3);

    }

    @Test
    public void testBoundsGuards() {
        final double [] p = new double [961];
        final double [] m = new double [961];
        final double [] l = new double [961];


        m[165] = 1;
        m[169] = 20;
        m[175] = 5;
        m[180] = 5.0;
        m[185] = 8.0;
        m[190] = 6.0;

        m[510] = 3.0;
        m[511] = 3.0;
        m[512] = 3.0;
        m[520] = 20.0;

        for (int t=170; t < 521; t++) {
            p[t] = 0.95;
        }

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m,l);

        TestCase.assertTrue(indicesOptional.isPresent());

        final EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(169,indices.iInBed,1);
        TestCase.assertEquals(indices.iInBed + 5,indices.iSleep,1);
        TestCase.assertEquals(520,indices.iWake,0);
        TestCase.assertEquals(521,indices.iOutOfBed,0);

    }

    @Test
    public void testNaturalTypicalDay() {
        final double [] p = new double [961];
        final double [] m = new double [961];
        final double [] m2 = new double [961];
        final double [] l = new double [961];


        m[165] = 1;
        m[169] = 20;
        m[175] = 5;
        m[180] = 5.0;
        m[185] = 8.0;
        m[190] = 6.0;

        m[510] = 3.0;
        m[511] = 3.0;
        m[512] = 3.0;
        m[520] = 20.0;

        for (int t = 200; t < 220; t++) {
            p[t] = 0.95 * (t-200) / 20.;
        }

        for (int t=220; t < 500; t++) {
            p[t] = 0.95;
        }

        for (int t = 500; t < 520; t++) {
            p[t] = 0.95 * (520 - t) / 20.;
        }

        m2[510] = 10.;
        m2[512] = 12.;

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m2,l);

        TestCase.assertTrue(indicesOptional.isPresent());

        final EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(indices.iInBed,169,1);
        TestCase.assertEquals(indices.iSleep,210,1);
        TestCase.assertEquals(indices.iWake,512,0);
        TestCase.assertEquals(indices.iOutOfBed,520,1);
    }

    @Test
    public void testNaturalTypicalDayWithTwoSegments() {
        final double [] p = new double [961];
        final double [] m = new double [961];
        final double [] m2 = new double [961];
        final double [] l = new double [961];


        m[165] = 1;
        m[169] = 20;
        m[175] = 5;
        m[180] = 5.0;
        m[185] = 8.0;
        m[190] = 6.0;

        m[510] = 3.0;
        m[511] = 3.0;
        m[512] = 3.0;
        m[520] = 20.0;

        for (int t = 200; t < 220; t++) {
            p[t] = 0.95 * (t-200) / 20.;
        }

        for (int t=220; t < 500; t++) {
            p[t] = 0.95;
        }

        for (int t = 500; t < 520; t++) {
            p[t] = 0.95 * (520 - t) / 20.;
        }

        m2[510] = 10.;
        m2[512] = 12.;

        for (int t = 560; t < 580; t++) {
            p[t] = 0.95 * (t-560) / 20.;
        }

        for (int t= 580; t < 600; t++) {
            p[t] = 0.95;
        }

        for (int t = 600; t < 620; t++) {
            p[t] = 0.95 * (620 - t) / 20.;
        }
        m2[605] = 5;
        m[605] = 5;
        m[610] = 5;

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m2,l);

        TestCase.assertTrue(indicesOptional.isPresent());

        final EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(indices.iInBed,169,1);
        TestCase.assertEquals(indices.iSleep,210,1);
        TestCase.assertEquals(indices.iWake,605,1);
        TestCase.assertEquals(indices.iOutOfBed,610,1);
    }

    @Test
    public void testNaturalTypicalDayWithTwoSegmentsWithSecondSegmentTooFarOut() {
        final double [] p = new double [961];
        final double [] m = new double [961];
        final double [] m2 = new double [961];
        final double [] l = new double [961];


        m[165] = 1;
        m[169] = 20;
        m[175] = 5;
        m[180] = 5.0;
        m[185] = 8.0;
        m[190] = 6.0;

        m[510] = 3.0;
        m[511] = 3.0;
        m[512] = 3.0;
        m[520] = 20.0;

        for (int t = 200; t < 220; t++) {
            p[t] = 0.95 * (t-200) / 20.;
        }

        for (int t=220; t < 500; t++) {
            p[t] = 0.95;
        }

        for (int t = 500; t < 520; t++) {
            p[t] = 0.95 * (520 - t) / 20.;
        }

        m2[510] = 10.;
        m2[512] = 12.;

        for (int t = 660; t < 680; t++) {
            p[t] = 0.95 * (t-660) / 20.;
        }

        for (int t= 680; t < 700; t++) {
            p[t] = 0.95;
        }

        for (int t = 700; t < 720; t++) {
            p[t] = 0.95 * (720 - t) / 20.;
        }
        m2[705] = 5;
        m[710] = 5;

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m2,l);

        TestCase.assertTrue(indicesOptional.isPresent());

        final EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(indices.iInBed,169,1);
        TestCase.assertEquals(indices.iSleep,210,1);
        TestCase.assertEquals(indices.iWake,512,1);
        TestCase.assertEquals(indices.iOutOfBed,517,10);
    }

    @Test
    public void testSleepSearch() {
        //final double [] sleepprobs, final double [] deltasleepprobs, final double [] pillMagnitude,final int begin, final int end) {

        //expect to ignore the sudden increase below p = 0.5
        final double [] sleepProbs = {0.0,0.0,0.0,0.1,0.4,0.5,0.5,0.7,0.8,0.9,1.0,1.0,1.0};
        final double [] dsleep = new double[sleepProbs.length];

        for (int i = 1; i < sleepProbs.length; i++) {
            dsleep[i] = sleepProbs[i] - sleepProbs[i-1];
        }

        final int idx = getSleepInInterval(sleepProbs,dsleep,0,sleepProbs.length-1);

        TestCase.assertEquals(7,idx,1);
    }

    @Test
    public void testWakeSearch() {
        //final double [] sleepprobs, final double [] deltasleepprobs, final double [] pillMagnitude,final int begin, final int end) {

        final double [] sleepProbs = {1.0,1.0,1.0,0.9,0.8,0.7,0.6,0.5,0.4,0.3,0.2,0.1,0.0,0.0,0.0};
        final double [] dsleep = new double[sleepProbs.length];
        final double [] mag = new double[sleepProbs.length];
        final double [] mag2 = new double[sleepProbs.length];
        final double [] l = new double [961];


        mag2[8] = 1.0;
        mag2[7] = 0.5;


        for (int i = 1; i < sleepProbs.length; i++) {
            dsleep[i] = sleepProbs[i] - sleepProbs[i-1];
        }

        //no pill motion, should default to first index
        final int idx = getWakeInInterval(mag,l,dsleep,2,sleepProbs.length-1);

        TestCase.assertEquals(2,idx);

        //should give index of maximum pill motion
        final int idx2 = getWakeInInterval(mag2,l,dsleep,0,sleepProbs.length-1);

        TestCase.assertEquals(8,idx2);


    }

    @Test
    public void testWakeSearchWithLightIncrease() {
        //final double [] sleepprobs, final double [] deltasleepprobs, final double [] pillMagnitude,final int begin, final int end) {

        final double [] sleepProbs = {1.0,1.0,1.0,0.9,0.8,0.7,0.6,0.5,0.4,0.3,0.2,0.1,0.0,0.0,0.0};
        final double [] dsleep = new double[sleepProbs.length];
        final double [] mag = new double[sleepProbs.length];
        final double [] mag2 = new double[sleepProbs.length];
        final double [] l = new double [961];


        mag2[8] = 1.0;
        mag2[7] = 0.5;
        l[7] = 1.5;


        for (int i = 1; i < sleepProbs.length; i++) {
            dsleep[i] = sleepProbs[i] - sleepProbs[i-1];
        }

        //should give index of maximum pill motion bounded by light
        final int idx2 = getWakeInInterval(mag2,l,dsleep,0,sleepProbs.length-1);

        TestCase.assertEquals(7,idx2);

        //no pill motion after lights on, should default to first index
        l[5] = 1.5;
        final int idx3 = getWakeInInterval(mag2,l,dsleep,1,sleepProbs.length-1);
        TestCase.assertEquals(1,idx3);


    }

    @Test
    public void testLowpassFilter() {
        final double [] x1 = {0.0,1.0,0.0};
        final double [] y1 = lowpassFilterSignal(x1,3);

        for (int i = 0; i < 3; i++) {
            TestCase.assertEquals(0.3333333333, y1[i], 1e-4);
        }


        final double [] x2 = new double[50];
        x2[10] = 1.0;
        x2[1] = 1.0;
        final double [] y2 = lowpassFilterSignal(x2,5);

        TestCase.assertTrue(y2[0] > 0.0);

        TestCase.assertEquals(y2[7],0.0,1e-6);
        TestCase.assertEquals(y2[8],0.2,1e-6);
        TestCase.assertEquals(y2[9],0.2,1e-6);
        TestCase.assertEquals(y2[10],0.2,1e-6);
        TestCase.assertEquals(y2[11],0.2,1e-6);
        TestCase.assertEquals(y2[12],0.2,1e-6);
        TestCase.assertEquals(y2[13],0.0,1e-6);

    }

}

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

        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(pnone,pnone,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(pnone,pnone,p).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(pnone,p,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(pnone,p,p).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(p,pnone,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(p,pnone,p).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(p,p,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreterWithSearch.getEventIndices(p,p,p).isPresent());

    }

    @Test
    public void testTypicalCase() {
        final double [] p = new double [961];
        final double [] m = new double [961];
        final double [] m2 = new double [961];


        for (int t=200; t < 500; t++) {
            p[t] = 0.95;
            m[t] = 1.0;
            m2[t] = 2.0;
        }

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m2);

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

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m);

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

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m);

        TestCase.assertTrue(indicesOptional.isPresent());

        final EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(indices.iInBed,indices.iSleep - SleepProbabilityInterpreterWithSearch.DEFAULT_SPACING_OF_IN_BED_BEFORE_SLEEP,2);
        TestCase.assertEquals(indices.iSleep,170,2);
        TestCase.assertEquals(indices.iWake,521,0);
        TestCase.assertEquals(indices.iWake + SleepProbabilityInterpreterWithSearch.DEFAULT_SPACING_OF_OUT_OF_BED_AFTER_WAKE,indices.iOutOfBed,1);

    }

    @Test
    public void testNaturalTypicalDay() {
        final double [] p = new double [961];
        final double [] m = new double [961];
        final double [] m2 = new double [961];

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

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m2);

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
        m[610] = 5;

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m2);

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

        final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices(p,m,m2);

        TestCase.assertTrue(indicesOptional.isPresent());

        final EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(indices.iInBed,169,1);
        TestCase.assertEquals(indices.iSleep,210,1);
        TestCase.assertEquals(indices.iWake,512,1);
        TestCase.assertEquals(indices.iOutOfBed,517,10);
    }

}

package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.interpretation.SleepProbabilityInterpreter;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by benjo on 3/24/16.
 */

public class SleepProbabilityInterpreterTest extends SleepProbabilityInterpreter {

    @Test
    public void testNoDataFail() {
        final double [] pnone = new double [0];
        final double [] p = new double [961];
        Arrays.fill(p,0.001);

        TestCase.assertFalse(SleepProbabilityInterpreter.getEventIndices(pnone,p).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreter.getEventIndices(pnone,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreter.getEventIndices(p,pnone).isPresent());
        TestCase.assertFalse(SleepProbabilityInterpreter.getEventIndices(p,p).isPresent());

    }

    @Test
    public void testTypicalCase() {
        final double [] p = new double [961];
        final double [] m = new double [961];


        for (int t=200; t < 500; t++) {
            p[t] = 0.95;
            m[t] = 1.0;
        }

        final Optional<SleepProbabilityInterpreter.EventIndices> indicesOptional = SleepProbabilityInterpreter.getEventIndices(p,m);

        TestCase.assertTrue(indicesOptional.isPresent());

        final SleepProbabilityInterpreter.EventIndices indices = indicesOptional.get();

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

        final Optional<SleepProbabilityInterpreter.EventIndices> indicesOptional = SleepProbabilityInterpreter.getEventIndices(p,m);

        TestCase.assertTrue(indicesOptional.isPresent());

        final SleepProbabilityInterpreter.EventIndices indices = indicesOptional.get();

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

        final Optional<SleepProbabilityInterpreter.EventIndices> indicesOptional = SleepProbabilityInterpreter.getEventIndices(p,m);

        TestCase.assertTrue(indicesOptional.isPresent());

        final SleepProbabilityInterpreter.EventIndices indices = indicesOptional.get();

        TestCase.assertEquals(indices.iInBed,indices.iSleep - SleepProbabilityInterpreter.DEFAULT_SPACING_OF_IN_BED_BEFORE_SLEEP,2);
        TestCase.assertEquals(indices.iSleep,170,2);
        TestCase.assertEquals(indices.iWake,521,0);
        TestCase.assertEquals(indices.iWake + SleepProbabilityInterpreter.DEFAULT_SPACING_OF_OUT_OF_BED_AFTER_WAKE,indices.iOutOfBed,1);

    }
}

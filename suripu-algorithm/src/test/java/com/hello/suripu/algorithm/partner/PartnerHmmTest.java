package com.hello.suripu.algorithm.partner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by benjo on 6/16/15.
 */
public class PartnerHmmTest {

    static void checkPath(final int [] ref, final ImmutableList<Integer> path) {
        TestCase.assertTrue(ref.length == path.size());

        for (int i = 0; i < ref.length; i++) {
            TestCase.assertEquals(ref[i],path.get(i).intValue());
        }
    }

    @Test
    public void testSimplePartnerStuff() {


        final PartnerHmm partnerHmmThingy = new PartnerHmm();

        final Double [] myMotionData1 = {0.0,0.0,1.0,1.0,1.0,0.0,0.0,0.0};
        final Double [] noPartnerMotion1 = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};

                                            //--------ME-ME-ME-ME-BOTH-BOTH--YOU-YOU---------------
        final Double [] myMotionData2 =      {0.0,0.0,1.0,1.0,1.0,0.0,1.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0};
        final Double [] somePartnerMotion2 = {0.0,0.0,0.0,0.0,0.0,1.0,1.0,0.0,1.0,1.0,0.0,0.0,0.0,0.0};

        final int [] refPath1 = {0,0,0,1,3,4,6,6};
        final int [] refPath2 = {0,0,0,1,1,1,3,3,3,5,5,6,6,6};
        final int [] refPath3 = {0,0,0,2,2,2,3,3,3,4,4,6,6,6};

        final ImmutableList<Integer> path1 = partnerHmmThingy.decodeSensorData(myMotionData1, noPartnerMotion1, 10);
        final ImmutableList<Integer> path2 = partnerHmmThingy.decodeSensorData(myMotionData2, somePartnerMotion2, 10);
        final ImmutableList<Integer> path3 = partnerHmmThingy.decodeSensorData(somePartnerMotion2, myMotionData2, 10);


        checkPath(refPath1,path1);
        checkPath(refPath2,path2);
        checkPath(refPath3,path3);


    }

    @Test
    public void testFeatureExtraction() {

        final int N = 4;
        final Double [] x1 = {1.0,2.0,3.0,4.0};
        final Double [] x2 = {3.0,3.0,3.0,3.0};


        for (int t = 0; t < N; t++) {
            final PartnerHmm.MeasurementPlusDebugInfo m1 = PartnerHmm.getMeasurementAsAlphabet(x1[t],x2[t]);
            final PartnerHmm.MeasurementPlusDebugInfo m2 = PartnerHmm.getMeasurementAsAlphabet(x2[t],x1[t]);


            if (!m1.frac.equals(0.0)) {
                TestCase.assertTrue(m1.frac.equals(-m2.frac));
            }


            if (m1.alphabet.equals(1.0)) {
                TestCase.assertTrue(m2.alphabet.equals(2.0));
            }
            else if (m1.alphabet.equals(2.0)) {
                TestCase.assertTrue(m2.alphabet.equals(1.0));
            }
            else if (m1.alphabet.equals(3.0)) {
                TestCase.assertTrue(m2.alphabet.equals(3.0));
            }
            else if (m1.alphabet.equals(0.0)) {
                TestCase.assertTrue(m2.alphabet.equals(0.0));
            }
        }


    }

}


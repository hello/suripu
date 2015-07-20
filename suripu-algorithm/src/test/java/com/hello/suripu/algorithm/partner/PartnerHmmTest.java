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

    @Test
    public void testSimplePartnerStuff() {


        final PartnerHmm partnerHmmThingy = new PartnerHmm();

        final Double [] myMotionData = {0.0,0.0,1.0,1.0,1.0,0.0,0.0,0.0};
        final Double [] partnerMotionData = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};

        final ImmutableList<Integer> path = partnerHmmThingy.decodeSensorData(myMotionData, partnerMotionData, 120);

        int foo = 3;
        foo++;

    }

}


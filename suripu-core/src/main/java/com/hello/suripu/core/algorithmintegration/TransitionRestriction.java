package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.hello.suripu.algorithm.hmm.Transition;

import java.util.Map;

/**
 * Created by benjo on 8/25/15.
 *
 * general interface for things that generate transition restrictions
 * one example of this is "MotionTransitionRestriction"
 *
 * the output result is forbidden transitions by time index
 */
public interface TransitionRestriction {
    Multimap<Integer,Transition> getRestrictions(final Map<String,ImmutableList<Integer>> features);
}

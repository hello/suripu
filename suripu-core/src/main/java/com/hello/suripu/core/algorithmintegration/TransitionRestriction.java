package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.hello.suripu.algorithm.hmm.Transition;

import java.util.Map;

/**
 * Created by benjo on 8/25/15.
 */
public interface TransitionRestriction {
    Multimap<Integer,Transition> getRestrictions(final Map<String,ImmutableList<Integer>> features);
}

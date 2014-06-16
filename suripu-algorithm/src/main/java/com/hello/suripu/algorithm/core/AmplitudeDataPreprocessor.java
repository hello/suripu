package com.hello.suripu.algorithm.core;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by pangwu on 6/11/14.
 */
public interface AmplitudeDataPreprocessor {
    ImmutableList<AmplitudeData> process(final List<AmplitudeData> rawData);
}

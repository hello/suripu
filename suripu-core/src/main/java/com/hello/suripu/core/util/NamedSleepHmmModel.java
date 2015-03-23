package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;

/**
 * Created by benjo on 3/16/15.
 */
public class NamedSleepHmmModel {
    public final HiddenMarkovModel hmm;
    public final String modelName;
    public final ImmutableSet<Integer> sleepStates;
    public final ImmutableSet<Integer> onBedStates;
    public final ImmutableSet<Integer> allowableEndingStates;
    public final ImmutableList<Integer> sleepDepthsByState;
    public final double soundDisturbanceThresholdDB;
    public final double pillMagnitudeDisturbanceThresholdLsb;
    public final double naturalLightFilterStartHour;
    public final double naturalLightFilterStopHour;
    public final int numMinutesInMeasPeriod;
    public final boolean isUsingIntervalSearch;


    public NamedSleepHmmModel(HiddenMarkovModel hmm,
                              String modelName,
                              ImmutableSet<Integer> sleepStates,
                              ImmutableSet<Integer> onBedStates,
                              ImmutableSet<Integer> allowableEndingStates,
                              ImmutableList<Integer> sleepDepthsByState,
                              double soundDisturbanceThresholdDB,
                              double pillMagnitudeDisturbanceThresholdLsb,
                              double naturalLightFilterStartHour,
                              double naturalLightFilterStopHour,
                              int numMinutesInMeasPeriod,
                              boolean isUsingIntervalSearch) {
        this.hmm = hmm;
        this.modelName = modelName;
        this.sleepStates = sleepStates;
        this.onBedStates = onBedStates;
        this.allowableEndingStates = allowableEndingStates;
        this.sleepDepthsByState = sleepDepthsByState;
        this.soundDisturbanceThresholdDB = soundDisturbanceThresholdDB;
        this.pillMagnitudeDisturbanceThresholdLsb = pillMagnitudeDisturbanceThresholdLsb;
        this.naturalLightFilterStartHour = naturalLightFilterStartHour;
        this.naturalLightFilterStopHour = naturalLightFilterStopHour;
        this.numMinutesInMeasPeriod = numMinutesInMeasPeriod;
        this.isUsingIntervalSearch = isUsingIntervalSearch;
    }
}

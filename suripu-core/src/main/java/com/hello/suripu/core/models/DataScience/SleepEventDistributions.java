package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 2/18/15.
 */
public class SleepEventDistributions {


    public SleepEventDistributions(final SleepEventPredictionDistribution inBedDistribution,
                                   final SleepEventPredictionDistribution sleepDistribution,
                                   final SleepEventPredictionDistribution wakeDistribution,
                                   final SleepEventPredictionDistribution outOfBedDistribution) {

        this.inBedDistribution = inBedDistribution;
        this.sleepDistribution = sleepDistribution;
        this.wakeDistribution = wakeDistribution;
        this.outOfBedDistribution = outOfBedDistribution;
    }

    @JsonProperty("in_bed_distribution")
    public final SleepEventPredictionDistribution inBedDistribution;

    @JsonProperty("fall_asleep_distribution")
    public final SleepEventPredictionDistribution sleepDistribution;

    @JsonProperty("awake_distribution")
    public final SleepEventPredictionDistribution wakeDistribution;

    @JsonProperty("out_of_bed_distribution")
    public final SleepEventPredictionDistribution outOfBedDistribution;



}

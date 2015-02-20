package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.Event;

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

    //for the priors, hour of the day
    public static double IN_BED_TIME = 21.0; //9pm, or 21:00
    public static double SLEEP_TIME = 22.0;
    public static double WAKE_TIME = 7.0;
    public static double OUT_OF_BED_TIME = 8.0;

    public static Event.Type SUPPORTED_EVENT_TYPES[] = {Event.Type.IN_BED,Event.Type.SLEEP,Event.Type.WAKE_UP,Event.Type.OUT_OF_BED};

    public static SleepEventDistributions getDefault() {
        return new SleepEventDistributions(
                SleepEventPredictionDistribution.getDefault(IN_BED_TIME),
                SleepEventPredictionDistribution.getDefault(SLEEP_TIME),
                SleepEventPredictionDistribution.getDefault(WAKE_TIME),
                SleepEventPredictionDistribution.getDefault(OUT_OF_BED_TIME));

    }


    public SleepEventDistributions getCopy() {
        return new SleepEventDistributions(this.inBedDistribution.getCopy(),
                                           this.sleepDistribution.getCopy(),
                                            this.wakeDistribution.getCopy(),
                                            this.outOfBedDistribution.getCopy());
    }

    public SleepEventDistributions getCopyWithPosteriorAsPrior() {
        return new SleepEventDistributions(this.inBedDistribution.getCopyWithPosteriorAsPrior(),
                this.sleepDistribution.getCopyWithPosteriorAsPrior(),
                this.wakeDistribution.getCopyWithPosteriorAsPrior(),
                this.outOfBedDistribution.getCopyWithPosteriorAsPrior());
    }

    //map from event enums
    public Optional<SleepEventPredictionDistribution> get(Event.Type type) {

        Optional<SleepEventPredictionDistribution> ret = Optional.absent();

        switch (type) {
            case IN_BED:
                ret = Optional.of(this.inBedDistribution);
                break;

            case SLEEP:
                ret = Optional.of(this.sleepDistribution);
                break;

            case WAKE_UP:
                ret = Optional.of(this.wakeDistribution);
                break;

            case OUT_OF_BED:
                ret = Optional.of(this.outOfBedDistribution);
                break;
        }

        return ret;
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

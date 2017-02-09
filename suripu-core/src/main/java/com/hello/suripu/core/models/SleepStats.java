package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class SleepStats {

    public static final int AWAKE_SLEEP_DEPTH_THRESHOLD = 5; // awake state < 5
    public static final int LIGHT_SLEEP_DEPTH_THRESHOLD = 10; // light < 10
    public static final int MEDIUM_SLEEP_DEPTH_THRESHOLD = 70; // medium < 70, sound >= 70

    @JsonProperty("sound_sleep")
    final public Integer soundSleepDurationInMinutes;

    @JsonIgnore
    final public Integer mediumSleepDurationInMinutes;

    @JsonIgnore
    final public Integer lightSleepDurationInMinutes;

    @JsonProperty("uninterrupted_sleep")
    final public Integer uninterruptedSleepDurationInMinutes;

    @JsonProperty("total_sleep")
    final public Integer sleepDurationInMinutes;

    @JsonProperty("times_awake")
    final public Integer numberOfMotionEvents;

    @JsonIgnore
    final public Long sleepTime;

    @JsonIgnore
    final public Long wakeTime;

    @JsonProperty("time_to_sleep")
    final public Integer sleepOnsetTimeMinutes;

    @JsonIgnore
    public final boolean isInBedDuration;

    public SleepStats(final Integer soundSleepDurationInMinutes,
                      final Integer mediumSleepDurationInMinutes,
                      final Integer lightSleepDurationInMinutes,
                      final Integer uninterruptedSleepDurationInMinutes,
                      final Integer sleepDurationInMinutes, final boolean isInBedDuration,
                      final Integer numberOfMotionEvents,
                      final Long sleepTime, final Long wakeTime, final Integer sleepOnsetTimeMinutes) {
        this.soundSleepDurationInMinutes = soundSleepDurationInMinutes;
        this.mediumSleepDurationInMinutes = mediumSleepDurationInMinutes;
        this.lightSleepDurationInMinutes = lightSleepDurationInMinutes;
        this.uninterruptedSleepDurationInMinutes = uninterruptedSleepDurationInMinutes;
        this.sleepDurationInMinutes = sleepDurationInMinutes;
        this.numberOfMotionEvents = numberOfMotionEvents;
        this.sleepTime = sleepTime;
        this.wakeTime = wakeTime;
        this.sleepOnsetTimeMinutes = sleepOnsetTimeMinutes;
        this.isInBedDuration = isInBedDuration;
    }

    public SleepStats(final Integer soundSleepDurationInMinutes,
                      final Integer mediumSleepDurationInMinutes,
                      final Integer lightSleepDurationInMinutes,
                      final Integer sleepDurationInMinutes, final boolean isInBedDuration,
                      final Integer numberOfMotionEvents,
                      final Long sleepTime, final Long wakeTime, final Integer sleepOnsetTimeMinutes) {
        this.soundSleepDurationInMinutes = soundSleepDurationInMinutes;
        this.mediumSleepDurationInMinutes = mediumSleepDurationInMinutes;
        this.lightSleepDurationInMinutes = lightSleepDurationInMinutes;
        this.uninterruptedSleepDurationInMinutes = soundSleepDurationInMinutes;
        this.sleepDurationInMinutes = sleepDurationInMinutes;
        this.numberOfMotionEvents = numberOfMotionEvents;
        this.sleepTime = sleepTime;
        this.wakeTime = wakeTime;
        this.sleepOnsetTimeMinutes = sleepOnsetTimeMinutes;
        this.isInBedDuration = isInBedDuration;
    }

    @JsonCreator
    public static SleepStats create(
            @JsonProperty("sound_sleep") Integer soundSleepDurationInMinutes,
            @JsonProperty("uninterrupted_sleep") Integer uninterruptedSleepDurationInMinutes,
            @JsonProperty("total_sleep") Integer sleepDurationInMinutes,
            @JsonProperty("times_awake") Integer numberOfMotionEvents,
            @JsonProperty("time_to_sleep") Integer sleepOnsetTimeMinutes) {

        return new SleepStats(soundSleepDurationInMinutes, 0, 0,uninterruptedSleepDurationInMinutes,
                sleepDurationInMinutes, true,
                numberOfMotionEvents, 0L, 0L, sleepOnsetTimeMinutes);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()
                .append("{soundSleep")
                .append(soundSleepDurationInMinutes)
                .append(",  mediumSleep")
                .append(mediumSleepDurationInMinutes)
                .append(", lightSleep")
                .append(lightSleepDurationInMinutes)
                .append(", uninterruptedSleep")
                .append(uninterruptedSleepDurationInMinutes)
                .append(", totalSleep")
                .append(sleepDurationInMinutes)
                .append(", # of motion events")
                .append(numberOfMotionEvents)
                .append(", sleep time")
                .append(sleepTime)
                .append(", wake time")
                .append(wakeTime)
                .append(", time to fall asleep")
                .append(sleepOnsetTimeMinutes)
                .append("}");
        return builder.toString();
    }

    @JsonIgnore
    public boolean isFromNull(){
        return Objects.equal(this.soundSleepDurationInMinutes, null);
    }
}
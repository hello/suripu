package com.hello.suripu.core.models;

import com.google.common.base.Objects;

public class SleepStats {

    final public Integer soundSleepDurationInMinutes;
    final public Integer lightSleepDurationInMinutes;
    final public Integer sleepDurationInMinutes;
    final public Integer numberOfMotionEvents;

    public SleepStats(final Integer soundSleepDurationInMinutes, final Integer lightSleepDurationInMinutes,
                      final Integer sleepDurationInMinutes, final Integer numberOfMotionEvents) {
        this.soundSleepDurationInMinutes = soundSleepDurationInMinutes;
        this.lightSleepDurationInMinutes = lightSleepDurationInMinutes;
        this.sleepDurationInMinutes = sleepDurationInMinutes;
        this.numberOfMotionEvents = numberOfMotionEvents;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(SleepStats.class)
                .add("soundSleep", sleepDurationInMinutes)
                .add("lightSleep", lightSleepDurationInMinutes)
                .add("totalSleep", sleepDurationInMinutes)
                .add("# of motion events", numberOfMotionEvents)
                .toString();
    }
}

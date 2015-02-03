package com.hello.suripu.core.models;

import com.google.common.base.Objects;

public class SleepStats {

    final public Integer soundSleepDurationInMinutes;
    final public Integer lightSleepDurationInMinutes;
    final public Integer sleepDurationInMinutes;
    final public Integer numberOfMotionEvents;
    final public Long sleepTime;
    final public Long wakeTime;
    final public Integer fallAsleepTime;

    public SleepStats(final Integer soundSleepDurationInMinutes, final Integer lightSleepDurationInMinutes,
                      final Integer sleepDurationInMinutes,
                      final Integer numberOfMotionEvents,
                      final Long sleepTime, final Long wakeTime, final Integer fallAsleepTime) {
        this.soundSleepDurationInMinutes = soundSleepDurationInMinutes;
        this.lightSleepDurationInMinutes = lightSleepDurationInMinutes;
        this.sleepDurationInMinutes = sleepDurationInMinutes;
        this.numberOfMotionEvents = numberOfMotionEvents;
        this.sleepTime = sleepTime;
        this.wakeTime = wakeTime;
        this.fallAsleepTime = fallAsleepTime;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(SleepStats.class)
                .add("soundSleep", sleepDurationInMinutes)
                .add("lightSleep", lightSleepDurationInMinutes)
                .add("totalSleep", sleepDurationInMinutes)
                .add("# of motion events", numberOfMotionEvents)
                .add("sleep time", sleepTime)
                .add("wake time", wakeTime)
                .add("time to fall asleep", fallAsleepTime)
                .toString();
    }
}

package com.hello.suripu.core.notifications.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class NotificationSchedule {

    @JsonProperty("hour")
    public final Integer hour;

    @JsonProperty("minute")
    public final Integer minute;

    public NotificationSchedule(Integer hour, Integer minute) {
        this.hour = hour;
        this.minute = minute;
    }

    @JsonCreator
    public static NotificationSchedule create(
            @JsonProperty("hour") Integer hour,
            @JsonProperty("minute") Integer minute) {
        return new NotificationSchedule(hour, minute);
    }

    @Override
    public String toString() {
        return String.format("%d:%d", hour, minute);
    }


    public static Optional<NotificationSchedule> fromString(String schedule) {
        if(schedule.isEmpty()) {
            return Optional.absent();
        }

        try {
            final String[] parts = schedule.split(":");
            return Optional.of(new NotificationSchedule(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1])
            ));
        } catch (Exception e) {
            return Optional.absent();
        }
    }
}

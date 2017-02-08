package com.hello.suripu.core.notifications.settings;

import com.google.common.base.Optional;

public class NotificationSchedule {

    public final Integer hour;
    public final Integer minute;

    public NotificationSchedule(Integer hour, Integer minute) {
        this.hour = hour;
        this.minute = minute;
    }


    @Override
    public String toString() {
        return String.format("%d:%d", hour, minute);
    }

    public static Optional<NotificationSchedule> fromString(String schedule) {
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

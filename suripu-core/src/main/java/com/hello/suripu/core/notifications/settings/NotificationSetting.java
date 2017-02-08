package com.hello.suripu.core.notifications.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

public class NotificationSetting {

    public enum Type {
        SLEEP_SCORE,
        SYSTEM,
        SLEEP_REMINDER
    }

    final Long accountId;
    final Optional<NotificationSchedule> schedule;

    private final String name;
    private final Type type;
    private final boolean enabled;


    public NotificationSetting(final Long accountId, final String name, final Type type, final boolean enabled, final Optional<NotificationSchedule> schedule) {
        this.accountId = accountId;
        this.name = name;
        this.type = type;
        this.enabled = enabled;
        this.schedule = schedule;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("type")
    public String type() {
        return type.toString();
    }

    @JsonProperty("enabled")
    public boolean enabled() {
        return enabled;
    }

    @JsonProperty("schedule")
    public NotificationSchedule schedule() {
        return schedule.orNull();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(NotificationSetting.class)
                .add("account_id", accountId)
                .add("name", name)
                .add("type", type)
                .add("enabled", enabled)
                .add("schedule", schedule.orNull())
                .toString();
    }

}

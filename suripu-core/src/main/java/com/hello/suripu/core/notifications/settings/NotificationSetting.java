package com.hello.suripu.core.notifications.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder= NotificationSetting.Builder.class)
public class NotificationSetting {

    @JsonIgnore
    public static final Map<Type, String> names = ImmutableMap.of(
            NotificationSetting.Type.SLEEP_SCORE, "Sleep Score",
            NotificationSetting.Type.SLEEP_REMINDER, "Sleep Reminder",
            NotificationSetting.Type.SYSTEM, "System Alerts"
    );

    public enum Type {
        SLEEP_SCORE("sleep_score"),
        SYSTEM("system"),
        SLEEP_REMINDER("sleep_reminder");

        private String value;
        Type(String value) {
            this.value = value;
        }

        @JsonCreator
        public static Type fromString(String value) {
            for(Type t : Type.values()) {
                if(t.value.equalsIgnoreCase(value)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("invalid notification type:" + value);
        }
    }

    @JsonIgnore
    final Optional<Long> accountId;

    @JsonIgnore
    final Optional<NotificationSchedule> schedule;

    final Type type;
    private final boolean enabled;


    public NotificationSetting(final Long accountId, final Type type, final boolean enabled, final Optional<NotificationSchedule> schedule) {
        this.accountId = Optional.fromNullable(accountId);
        this.type = type;
        this.enabled = enabled;
        this.schedule = schedule;
    }

    @JsonProperty("name")
    public String name() {
        return names.getOrDefault(type, "");
    }

    @JsonProperty("type")
    public String type() {
        return type.name();
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
                .add("account_id", accountId.or(0L))
                .add("type", type)
                .add("enabled", enabled)
                .add("schedule", schedule.orNull())
                .toString();
    }

    public static NotificationSetting withAccount(final NotificationSetting setting, final Long accountId) {
        return new NotificationSetting(accountId,setting.type,setting.enabled,setting.schedule);
    }

    public static class Builder {

        private Long accountId = null;
        private Type type;
        private boolean enabled;
        private Optional<NotificationSchedule> schedule = Optional.absent();

        Builder() {

            enabled = false;
        }

        @JsonProperty("type")
        public Builder withType(final String type) {
            this.type = Type.fromString(type);
            return this;
        }

        @JsonIgnore
        public Builder withType(final Type type) {
            this.type = type;
            return this;
        }

        @JsonProperty("enabled")
        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @JsonProperty("schedule")
        public Builder withSchedule(NotificationSchedule schedule) {
            this.schedule = Optional.fromNullable(schedule);
            return this;
        }

        public NotificationSetting build() {
            return new NotificationSetting(accountId, type, enabled, schedule);
        }
    }
}

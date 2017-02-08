package com.hello.suripu.core.notifications.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationSetting {

    @JsonIgnore
    public static final Map<Type, String> names = ImmutableMap.of(
            NotificationSetting.Type.SLEEP_SCORE, "Sleep Score",
            NotificationSetting.Type.SLEEP_REMINDER, "Sleep Reminder",
            NotificationSetting.Type.SYSTEM, "System"
    );

    public enum Type {
        SLEEP_SCORE,
        SYSTEM,
        SLEEP_REMINDER
    }

    @JsonIgnore
    final Optional<Long> accountId;

    final Optional<NotificationSchedule> schedule;

    private final Type type;
    private final boolean enabled;


    public NotificationSetting(final Long accountId, final Type type, final boolean enabled, final Optional<NotificationSchedule> schedule) {
        this.accountId = Optional.fromNullable(accountId);
        this.type = type;
        this.enabled = enabled;
        this.schedule = schedule;
    }

    @JsonCreator
    public static NotificationSetting create(
            @JsonProperty("type") String type,
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("schedule") NotificationSchedule schedule) {
        return new NotificationSetting(null, Type.valueOf(type), enabled, Optional.fromNullable(schedule));
    }

    @JsonProperty("name")
    public String name() {
        return names.getOrDefault(type, "");
    }

    @JsonProperty("type")
    public Type type() {
        return type;
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
        return new NotificationSetting(
                accountId,
                setting.type,
                setting.enabled,
                setting.schedule
        );
    }



}

package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/**
 * Created by pangwu on 5/27/15.
 */
public class SmartAlarmHistory {
    @JsonProperty("account_id")
    public final long accountId;

    @JsonProperty("scheduled_at_local")
    public final String scheduledAtLocal;

    @JsonProperty("expected_ringtime_local")
    public final String expectedRingTimeLocal;

    @JsonProperty("actual_ringtime_local")
    public final String actualRingTimeLocal;

    @JsonProperty("last_sleep_cycle_local")
    public final String lastSleepCycleLocal;

    @JsonProperty("timezone_id")
    public final Optional<String> timeZoneId;


    private SmartAlarmHistory(final long accountId,
                              final String scheduledAtLocal,
                              final String expectedRingTimeLocal,
                              final String actualRingTimeLocal,
                              final String lastSleepCycleLocal,
                              final Optional<String> timeZoneId){
        this.accountId = accountId;
        this.actualRingTimeLocal = actualRingTimeLocal;
        this.expectedRingTimeLocal = expectedRingTimeLocal;
        this.scheduledAtLocal = scheduledAtLocal;
        this.lastSleepCycleLocal = lastSleepCycleLocal;
        this.timeZoneId = timeZoneId;
    }

    @JsonCreator
    public static SmartAlarmHistory create(@JsonProperty("account_id") final long accountId,
                                    @JsonProperty("scheduled_at_local") final String scheduledAtLocal,
                                    @JsonProperty("expected_ringtime_local") final String expectedRingTimeLocal,
                                    @JsonProperty("actual_ringtime_local") final String actualRingTimeLocal,
                                    @JsonProperty("last_sleep_cycle_local") final String lastSleepCycleLocal,
                                    @JsonProperty("timezone_id") final String timeZoneId){
        final SmartAlarmHistory smartAlarmHistory = new SmartAlarmHistory(accountId,
                scheduledAtLocal, expectedRingTimeLocal, actualRingTimeLocal, lastSleepCycleLocal,
                Optional.fromNullable(timeZoneId));
        return smartAlarmHistory;
    }
}

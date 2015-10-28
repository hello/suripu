package com.hello.suripu.service.configuration;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class SenseUploadConfiguration extends Configuration {
    private static final Integer DEFAULT_NON_PEAK_HOUR_LOWER_BOUND = 11;  // non peak periods start at 11:00:00
    private static final Integer DEFAULT_NON_PEAK_HOUR_UPPER_BOUND = 22;  // non peak periods end at 22:59:59
    private static final Boolean DEFAULT_WEEK_DAYS_ONLY = true;           // weekends are treated as peak periods
    public static final Integer INCREASED_INTERVAL_NON_PEAK = 6;    // minutes
    public static final Integer INCREASED_INTERVAL_PEAK = 2;   // minutes
    public static final Integer DEFAULT_UPLOAD_INTERVAL = 1; // minutes

    @JsonProperty("non_peak_hour_lower_bound")
    private Integer nonPeakHourLowerBound = DEFAULT_NON_PEAK_HOUR_LOWER_BOUND;

    @JsonProperty("non_peak_hour_upper_bound")
    private Integer nonPeakHourUpperBound = DEFAULT_NON_PEAK_HOUR_UPPER_BOUND;

    @JsonProperty("week_days_only")
    private Boolean weekDaysOnly = DEFAULT_WEEK_DAYS_ONLY;

    @Valid
    @Max(10)
    @Min(1)
    @JsonProperty("increased_non_peak_interval")
    private Integer increasedNonPeakInterval = INCREASED_INTERVAL_NON_PEAK;

    @Valid
    @Max(10)
    @Min(1)
    @JsonProperty("increased_peak_interval")
    private Integer increasedPeakInterval = INCREASED_INTERVAL_PEAK;

    @Valid
    @Max(10)
    @Min(1)
    @JsonProperty("default_interval")
    private Integer defaultInterval = DEFAULT_UPLOAD_INTERVAL;

    public Integer getNonPeakHourLowerBound() {
        return this.nonPeakHourLowerBound;
    }

    public Integer getNonPeakHourUpperBound() {
        return this.nonPeakHourUpperBound;
    }

    public Boolean getWeekDaysOnly() {
        return this.weekDaysOnly;
    }

    public Integer getDefaultUploadInterval() { return this.defaultInterval; }
    public Integer getIncreasedNonPeakUploadInterval() { return this.increasedNonPeakInterval; }
    public Integer getIncreasedPeakUploadInterval() {
        return this.increasedPeakInterval;
    }
}

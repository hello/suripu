package com.hello.suripu.service.configuration;


import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@JsonFilter("filter properties by name")
public class SenseUploadConfiguration extends Configuration {
    private static final Integer DEFAULT_NON_PEAK_HOUR_LOWER_BOUND = 11;  // non peak periods start at 11:00:00
    private static final Integer DEFAULT_NON_PEAK_HOUR_UPPER_BOUND = 22;  // non peak periods end at 22:59:59
    private static final Boolean DEFAULT_WEEK_DAYS_ONLY = true;           // weekends are treated as peak periods
    private static final Integer DEFAULT_LONG_INTERVAL = 6;    // minutes
    private static final Integer DEFAULT_SHORT_INTERVAL = 2;   // minutes
    public static final Integer REDUCED_LONG_INTERVAL = 2;    // minutes
    public static final Integer REDUCED_SHORT_INTERVAL = 1;   // minutes


    @JsonProperty("non_peak_hour_lower_bound")
    private Integer nonPeakHourLowerBound = DEFAULT_NON_PEAK_HOUR_LOWER_BOUND;

    @JsonProperty("non_peak_hour_upper_bound")
    private Integer nonPeakHourUpperBound = DEFAULT_NON_PEAK_HOUR_UPPER_BOUND;

    @JsonProperty("week_days_only")
    private Boolean weekDaysOnly = DEFAULT_WEEK_DAYS_ONLY;

    @Valid
    @Max(10)
    @Min(1)
    @JsonProperty("long_interval")
    private Integer longInterval = DEFAULT_LONG_INTERVAL;

    @Valid
    @Max(10)
    @Min(1)
    @JsonProperty("short_interval")
    private Integer shortInterval = DEFAULT_SHORT_INTERVAL;

    public Integer getNonPeakHourLowerBound() {
        return this.nonPeakHourLowerBound;
    }

    public Integer getNonPeakHourUpperBound() {
        return this.nonPeakHourUpperBound;
    }

    public Boolean getWeekDaysOnly() {
        return this.weekDaysOnly;
    }

    public Integer getLongInterval() {
        return this.longInterval;
    }

    public Integer getShortInterval() {
        return this.shortInterval;
    }
}

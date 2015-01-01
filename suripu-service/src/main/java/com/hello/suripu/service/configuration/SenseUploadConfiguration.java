package com.hello.suripu.service.configuration;


public class SenseUploadConfiguration {
    private static final Integer DEFAULT_NON_PEAK_HOUR_LOWER_BOUND = 11;  // non peak periods start at 11:00:00
    private static final Integer DEFAULT_NON_PEAK_HOUR_UPPER_BOUND = 20;  // non peak periods end at 20:00:59
    private static final Boolean DEFAULT_WEEKS_DAY_ONLY = true;           // weekends are treated as peak periods
    private static final Integer DEFAULT_LONG_INTERVAL = 6;    // minutes
    private static final Integer DEFAULT_SHORT_INTERVAL = 2;   // minutes

    public final Integer nonPeakHourLowerBound;
    public final Integer nonPeakHourUpperBound;
    public final Boolean weekDaysOnly;
    public final Integer longInterval;
    public final Integer shortInterval;

    public SenseUploadConfiguration(final Integer nonPeakHourLowerBound,
                                    final Integer nonPeakHourUpperBound,
                                    final Boolean weekDaysOnly,
                                    final Integer longInterval,
                                    final Integer shortInterval) {
        this.nonPeakHourLowerBound = nonPeakHourLowerBound;
        this.nonPeakHourUpperBound = nonPeakHourUpperBound;
        this.weekDaysOnly = weekDaysOnly;
        this.longInterval = longInterval;
        this.shortInterval = shortInterval;
    }

    public SenseUploadConfiguration() {
        this(DEFAULT_NON_PEAK_HOUR_LOWER_BOUND, DEFAULT_NON_PEAK_HOUR_UPPER_BOUND, DEFAULT_WEEKS_DAY_ONLY, DEFAULT_LONG_INTERVAL, DEFAULT_SHORT_INTERVAL);
    }
}

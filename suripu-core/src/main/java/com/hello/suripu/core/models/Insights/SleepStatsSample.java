package com.hello.suripu.core.models.Insights;

import com.hello.suripu.core.models.SleepStats;
import org.joda.time.DateTime;

/**
 * Created by kingshy on 12/19/14.
 */
public class SleepStatsSample {
    public final SleepStats stats;
    public final DateTime localUTCDate;
    public final int timeZoneOffset;

    public SleepStatsSample(final SleepStats stats, final DateTime localUTCDate, final int timeZoneOffset) {
        this.stats = stats;
        this.localUTCDate = localUTCDate;
        this.timeZoneOffset = timeZoneOffset;
    }
}

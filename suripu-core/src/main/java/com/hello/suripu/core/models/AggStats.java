package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

/**
 * Created by jyfan on 7/1/16.
 *
 * We define a day as a roughly 24-hour period of daytime hours plus the immediately preceding hours of sleep at night.
 * Example day ranges [2016-07-01 12noon, 2016-07-02 12noon) referred to by the date "2016-07-01" (user's local time)
 *
 */
public class AggStats {
    //Definitions https://hello.hackpad.com/agg_stats-KETKVqohjiv

    public static final int DAY_START_END_HOUR = 12; //a day begins at 12noon and ends at 12noon the next date

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("date_local")
    public final DateTime dateLocal;

    //all raw values straight from device_data and pill_data dbs
    @JsonProperty("avg_daily_temp")
    public final int avgDailyTemp;

    @JsonProperty("max_daily_temp")
    public final int maxDailyTemp;

    @JsonProperty("min_daily_temp")
    public final int minDailyTemp;

    @JsonProperty("avg_daily_humidity")
    public final int avgDailyHumidity;

    @JsonProperty("avg_daily_raw_dust")
    public final int avgDailyRawDust;


    public AggStats(final Long accountId,
                    final DateTime dateLocal,
                    final int avgDailyTemp,
                    final int maxDailyTemp,
                    final int minDailyTemp,
                    final int avgDailyHumidity,
                    final int avgDailyRawDust) {
        this.accountId = accountId;
        this.dateLocal = dateLocal;
        this.avgDailyTemp = avgDailyTemp;
        this.maxDailyTemp = maxDailyTemp;
        this.minDailyTemp = minDailyTemp;
        this.avgDailyHumidity = avgDailyHumidity;
        this.avgDailyRawDust = avgDailyRawDust;
    }
}

package com.hello.suripu.core.util;

import com.google.common.annotations.VisibleForTesting;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.TrackerMotion;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by jyfan on 7/5/16.
 */
public class AggStatsUtils {

    public static AggStats computeAggStats(final Long accountId, final DateTime dateLocal, final List<DeviceData> deviceDatas, final List<TrackerMotion> trackerMotions) {

//        final DescriptiveStatistics lightStats = new DescriptiveStatistics();
        final DescriptiveStatistics tempStats = new DescriptiveStatistics();
        final DescriptiveStatistics humidStats = new DescriptiveStatistics();
        final DescriptiveStatistics rawDustStats = new DescriptiveStatistics();
//        final DescriptiveStatistics audioStats = new DescriptiveStatistics();

        for (DeviceData deviceData : deviceDatas) {
            tempStats.addValue(deviceData.ambientTemperature);
            humidStats.addValue(deviceData.ambientHumidity);
            rawDustStats.addValue(deviceData.ambientAirQualityRaw);
        }

        final int avg_daily_temp = ((int) tempStats.getMean());
        final int max_daily_temp = ((int) tempStats.getMax());
        final int min_daily_temp = ((int) tempStats.getMin());
        final int avg_daily_humidity = ((int) humidStats.getMean());
        final int avg_daily_raw_dust = ((int) rawDustStats.getMean());

        return new AggStats(accountId, dateLocal, avg_daily_temp, max_daily_temp, min_daily_temp, avg_daily_humidity, avg_daily_raw_dust);
    }

}

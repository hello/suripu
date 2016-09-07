package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.AggStatsInputs;
import com.hello.suripu.core.models.Insights.SumCountData;
import com.hello.suripu.core.sense.data.AggStatDeviceData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jyfan on 7/5/16.
 */
public class AggStatsComputer {

    public static final int TO_MICRO_CONVERSION = 1000000;
    public static final List<Integer> LIGHT_HOUR_BUCKETS_LIST = ImmutableList.copyOf(Lists.newArrayList(22, 23, 0, 1, 2, 3, 4, 5));

    public static Optional<AggStats> computeAggStats(final Long accountId, final DeviceId deviceId, final DateTime dateLocal, final AggStatsInputs aggStatsInputs) {
        if (aggStatsInputs.aggStatDeviceDataList.isEmpty()) {
            return Optional.absent();
        }

        //Initialize descriptive stats
        final DescriptiveStatistics tempRawStats = new DescriptiveStatistics();
        final DescriptiveStatistics humidRawStats = new DescriptiveStatistics();
        final DescriptiveStatistics dustRawStats = new DescriptiveStatistics();

        final Map<Integer, DescriptiveStatistics> lightHourDescriptiveStatistics = new HashMap<>();
        for (final Integer hour : LIGHT_HOUR_BUCKETS_LIST) {
            lightHourDescriptiveStatistics.put(hour, new DescriptiveStatistics());
        }

        //Add data to descriptive stats
        for (final AggStatDeviceData aggStatDeviceData : aggStatsInputs.aggStatDeviceDataList) {
            tempRawStats.addValue(aggStatDeviceData.ambientTemperature); //TODO: edit
            humidRawStats.addValue(aggStatDeviceData.ambientHumidity);
            dustRawStats.addValue(aggStatDeviceData.ambientAirQualityRaw);

            final int hour = aggStatDeviceData.localTime.hourOfDay().get();
            if (lightHourDescriptiveStatistics.containsKey(hour)) {
                lightHourDescriptiveStatistics.get(hour).addValue(aggStatDeviceData.ambientLightFloat);
            }
        }

        //Compute relevant stats
        final int deviceDataSize = aggStatsInputs.aggStatDeviceDataList.size();
        final int trackerMotionSize = aggStatsInputs.aggStatPillDataList.size();

        final int avg_daily_temp_raw = (int) tempRawStats.getMean();
        final int max_daily_temp_raw = (int) tempRawStats.getMax();
        final int min_daily_temp_raw = (int) tempRawStats.getMin();
        final int avg_daily_temp = floatToMicroInt(DataUtils.calibrateTemperature(avg_daily_temp_raw));
        final int max_daily_temp = floatToMicroInt(DataUtils.calibrateTemperature(max_daily_temp_raw));
        final int min_daily_temp = floatToMicroInt(DataUtils.calibrateTemperature(min_daily_temp_raw));

        final int avg_daily_humidity_raw = (int) humidRawStats.getMean();
        final int avg_daily_humidity = floatToMicroInt(DataUtils.calibrateHumidity(avg_daily_temp_raw, avg_daily_humidity_raw));

        final int avg_daily_dust_raw = ((int) dustRawStats.getMean());
        final int avg_daily_dust = floatToMicroInt(DataUtils.convertRawDustCountsToDensity(avg_daily_dust_raw, aggStatsInputs.calibrationOptional));

        final Device.Color color = aggStatsInputs.senseColorOptional.or(Device.Color.WHITE); //default color white, nothing is done in calibration

        final Map<Integer, SumCountData> microLuxSumCountHourMap = new HashMap<>();
        for (final Map.Entry<Integer, DescriptiveStatistics> entry : lightHourDescriptiveStatistics.entrySet()) {

            final float sumHourLightRaw = (float) entry.getValue().getSum();
            final int sumMicroLux = floatToMicroInt(DataUtils.calibrateLight(sumHourLightRaw, color));
            final int countMicroLux = (int) entry.getValue().getN();
            microLuxSumCountHourMap.put(entry.getKey(), new SumCountData(sumMicroLux, countMicroLux));
        }

        //Build
        final AggStats.Builder aggStatsBuilder = new AggStats.Builder()
                .withAccountId(accountId)
                .withDateLocal(dateLocal)
                .withExternalDeviceId(deviceId.externalDeviceId.get())

                .withDeviceDataCount(Optional.of(deviceDataSize))
                .withTrackerMotionCount(Optional.of(trackerMotionSize))

                .withAvgDailyTemp(Optional.of(avg_daily_temp))
                .withMaxDailyTemp(Optional.of(max_daily_temp))
                .withMinDailyTemp(Optional.of(min_daily_temp))
                .withAvgDailyHumidity(Optional.of(avg_daily_humidity))
                .withAvgDailyDustDensity(Optional.of(avg_daily_dust))

                .withSumCountMicroLuxHourMap(microLuxSumCountHourMap);

        final AggStats aggStats =  aggStatsBuilder.build();
        return Optional.of(aggStats);
    }

    private static int floatToMicroInt(final float datum) {
        return (int) (datum * TO_MICRO_CONVERSION);
    }

}

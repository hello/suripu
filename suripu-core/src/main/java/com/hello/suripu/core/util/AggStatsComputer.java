package com.hello.suripu.core.util;

import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.AggStatsInputs;
import com.hello.suripu.core.models.Insights.SumLengthData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jyfan on 7/5/16.
 */
public class AggStatsComputer {

    public static final int TO_MICRO_CONVERSION = 1000000;

    public static AggStats computeAggStats(final Long accountId, final DeviceId deviceId, final DateTime dateLocal, final AggStatsInputs aggStatsInputs) {

        //Initialize descriptive stats
        final DescriptiveStatistics tempRawStats = new DescriptiveStatistics();
        final DescriptiveStatistics humidRawStats = new DescriptiveStatistics();
        final DescriptiveStatistics dustRawStats = new DescriptiveStatistics();

        final Map<Integer, DescriptiveStatistics> lightHourDescriptiveStatistics = new HashMap<>();
        for (final Integer hour : AggStats.LIGHT_HOUR_BUCKETS) {
            lightHourDescriptiveStatistics.put(hour, new DescriptiveStatistics());
        }

        //Add data to descriptive stats
        for (final DeviceData deviceData : aggStatsInputs.deviceDataList) {
            tempRawStats.addValue(deviceData.ambientTemperature);
            humidRawStats.addValue(deviceData.ambientHumidity);
            dustRawStats.addValue(deviceData.ambientAirQualityRaw);

            final int hour = deviceData.localTime().hourOfDay().get();
            if (lightHourDescriptiveStatistics.containsKey(hour)) {
                lightHourDescriptiveStatistics.get(hour).addValue(deviceData.ambientLightFloat);
            }
        }

        //Compute relevant stats
        final int deviceDataSize = aggStatsInputs.deviceDataList.size();
        final int trackerMotionSize = aggStatsInputs.pillDataList.size();

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

        final Map<Integer, SumLengthData> microLuxSumLengthHourMap = new HashMap<>();
        for (final Map.Entry<Integer, DescriptiveStatistics> entry : lightHourDescriptiveStatistics.entrySet()) {

            final float sumHourLightRaw = (float) entry.getValue().getSum();
            final int sumMicroLux = floatToMicroInt(DataUtils.calibrateLight(sumHourLightRaw, color));
            final int lengthMicroLux = (int) entry.getValue().getN();
            microLuxSumLengthHourMap.put(entry.getKey(), new SumLengthData(sumMicroLux, lengthMicroLux));
        }

        //Build
        final AggStats.Builder aggStatsBuilder = new AggStats.Builder()
                .withAccountId(accountId)
                .withDateLocal(dateLocal)
                .withExternalDeviceId(deviceId.externalDeviceId.get())

                .withDeviceDataLength(deviceDataSize)
                .withTrackerMotionLength(trackerMotionSize)

                .withAvgDailyTemp(avg_daily_temp)
                .withMaxDailyTemp(max_daily_temp)
                .withMinDailyTemp(min_daily_temp)
                .withAvgDailyHumidity(avg_daily_humidity)
                .withAvgDailyDustDensity(avg_daily_dust)

                .withSumLenMicroLuxHourMap(microLuxSumLengthHourMap);

        return aggStatsBuilder.build();
    }

    private static int floatToMicroInt(final float datum) {
        return (int) (datum * TO_MICRO_CONVERSION);
    }

}

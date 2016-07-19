package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.SumLengthData;
import com.hello.suripu.core.models.TrackerMotion;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by jyfan on 7/5/16.
 */
public class AggStatsComputer {

    public static final int TO_MICRO_CONVERSION = 1000000;

    public static AggStats computeAggStats(final Long accountId, final DeviceId deviceId, final DateTime dateLocal, final List<DeviceData> deviceDatas, final List<TrackerMotion> trackerMotions, final Optional<Device.Color> colorOptional, final Optional<Calibration> airQualityCalibrationOptional) {

        final DescriptiveStatistics tempRawStats = new DescriptiveStatistics();
        final DescriptiveStatistics humidRawStats = new DescriptiveStatistics();
        final DescriptiveStatistics dustRawStats = new DescriptiveStatistics();

        final DescriptiveStatistics lightRawHr22Stats = new DescriptiveStatistics();
        final DescriptiveStatistics lightRawHr23Stats = new DescriptiveStatistics();
        final DescriptiveStatistics lightRawHr0Stats = new DescriptiveStatistics();
        final DescriptiveStatistics lightRawHr1Stats = new DescriptiveStatistics();
        final DescriptiveStatistics lightRawHr2Stats = new DescriptiveStatistics();
        final DescriptiveStatistics lightRawHr3Stats = new DescriptiveStatistics();
        final DescriptiveStatistics lightRawHr4Stats = new DescriptiveStatistics();
        final DescriptiveStatistics lightRawHr5Stats = new DescriptiveStatistics();

        for (DeviceData deviceData : deviceDatas) {
            tempRawStats.addValue(deviceData.ambientTemperature);
            humidRawStats.addValue(deviceData.ambientHumidity);
            dustRawStats.addValue(deviceData.ambientAirQualityRaw);

            final int hour = deviceData.localTime().hourOfDay().get();

            if (hour == 22) {
                lightRawHr22Stats.addValue(deviceData.ambientLightFloat);
            } else if (hour == 23) {
                lightRawHr23Stats.addValue(deviceData.ambientLightFloat);
            } else if (hour == 0) {
                lightRawHr0Stats.addValue(deviceData.ambientLightFloat);
            } else if (hour == 1) {
                lightRawHr1Stats.addValue(deviceData.ambientLightFloat);
            } else if (hour == 2) {
                lightRawHr2Stats.addValue(deviceData.ambientLightFloat);
            } else if (hour == 3) {
                lightRawHr3Stats.addValue(deviceData.ambientLightFloat);
            } else if (hour == 4) {
                lightRawHr4Stats.addValue(deviceData.ambientLightFloat);
            } else if (hour == 5) {
                lightRawHr5Stats.addValue(deviceData.ambientLightFloat);
            }
        }

        final int deviceDataSize = deviceDatas.size();
        final int trackerMotionSize = trackerMotions.size();

        final int avg_daily_temp_raw = ((int) tempRawStats.getMean());
        final int max_daily_temp_raw = ((int) tempRawStats.getMax());
        final int min_daily_temp_raw = ((int) tempRawStats.getMin());
        final int avg_daily_temp = floatToMicroInt(DataUtils.calibrateTemperature(avg_daily_temp_raw));
        final int max_daily_temp = floatToMicroInt(DataUtils.calibrateTemperature(max_daily_temp_raw));
        final int min_daily_temp = floatToMicroInt(DataUtils.calibrateTemperature(min_daily_temp_raw));

        final int avg_daily_humidity_raw = (int) humidRawStats.getMean();
        final int avg_daily_humidity = floatToMicroInt(DataUtils.calibrateHumidity(avg_daily_temp_raw, avg_daily_humidity_raw));

        final int avg_daily_dust_raw = ((int) dustRawStats.getMean());
        final int avg_daily_dust = floatToMicroInt(DataUtils.convertRawDustCountsToDensity(avg_daily_dust_raw, airQualityCalibrationOptional));

        final Device.Color color;
        if (colorOptional.isPresent()) {
            color = colorOptional.get();
        } else {
            color = Device.Color.WHITE; //default color, nothing is done in calibration
        }

        final SumLengthData sum_len_light_22 = new SumLengthData( floatToMicroInt(DataUtils.calibrateLight((float) lightRawHr22Stats.getSum(), color)), (int) lightRawHr22Stats.getN());
        final SumLengthData sum_len_light_23 = new SumLengthData( floatToMicroInt(DataUtils.calibrateLight((float) lightRawHr23Stats.getSum(), color)), (int) lightRawHr23Stats.getN());
        final SumLengthData sum_len_light_0 = new SumLengthData( floatToMicroInt(DataUtils.calibrateLight((float) lightRawHr0Stats.getSum(), color)), (int) lightRawHr0Stats.getN());
        final SumLengthData sum_len_light_1 = new SumLengthData( floatToMicroInt(DataUtils.calibrateLight((float) lightRawHr1Stats.getSum(), color)), (int) lightRawHr1Stats.getN());
        final SumLengthData sum_len_light_2 = new SumLengthData( floatToMicroInt(DataUtils.calibrateLight((float) lightRawHr2Stats.getSum(), color)), (int) lightRawHr2Stats.getN());
        final SumLengthData sum_len_light_3 = new SumLengthData( floatToMicroInt(DataUtils.calibrateLight((float) lightRawHr3Stats.getSum(), color)), (int) lightRawHr3Stats.getN());
        final SumLengthData sum_len_light_4 = new SumLengthData( floatToMicroInt(DataUtils.calibrateLight((float) lightRawHr4Stats.getSum(), color)), (int) lightRawHr4Stats.getN());
        final SumLengthData sum_len_light_5 = new SumLengthData( floatToMicroInt(DataUtils.calibrateLight((float) lightRawHr5Stats.getSum(), color)), (int) lightRawHr5Stats.getN());

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

                .withSumLenMicroLux22(sum_len_light_22)
                .withSumLenMicroLux23(sum_len_light_23)
                .withSumLenMicroLux0(sum_len_light_0)
                .withSumLenMicroLux1(sum_len_light_1)
                .withSumLenMicroLux2(sum_len_light_2)
                .withSumLenMicroLux3(sum_len_light_3)
                .withSumLenMicroLux4(sum_len_light_4)
                .withSumLenMicroLux5(sum_len_light_5);

        return aggStatsBuilder.build();
    }

    private static int floatToMicroInt(final float datum) {
        return (int) (datum * TO_MICRO_CONVERSION);
    }

}

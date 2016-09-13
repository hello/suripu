package com.hello.suripu.core.sense.data;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceData;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by jyfan on 9/6/16.
 */
public class AggStatDeviceData {

    public final Integer ambientTemperature;
    public final Integer ambientHumidity;
    public final Integer ambientAirQualityRaw;
    public final Float ambientLightFloat;
    public final DateTime localTime;

    public AggStatDeviceData(final Integer ambientTemperature,
                                   final Integer ambientHumidity,
                                   final Integer ambientAirQualityRaw,
                                   final Float ambientLightFloat,
                                   final DateTime localTime) {
        this.ambientTemperature = ambientTemperature;
        this.ambientHumidity = ambientHumidity;
        this.ambientAirQualityRaw = ambientAirQualityRaw;
        this.ambientLightFloat = ambientLightFloat;
        this.localTime = localTime;
    }

    public static List<AggStatDeviceData> createAggStatDeviceDataList(final List<DeviceData> deviceDatas) {

        final List<AggStatDeviceData> aggStatDeviceDatas = Lists.newArrayList();

        for (final DeviceData deviceData : deviceDatas) {
            final AggStatDeviceData aggStatDeviceData = new AggStatDeviceData(
                    deviceData.ambientTemperature,
                    deviceData.ambientHumidity,
                    deviceData.ambientAirQualityRaw,
                    deviceData.ambientLightFloat,
                    deviceData.localTime());

            aggStatDeviceDatas.add(aggStatDeviceData);
        }

        return aggStatDeviceDatas;
    }

}

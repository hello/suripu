package com.hello.suripu.core.models.Insights;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.TrackerMotion;

/**
 * Created by jyfan on 7/25/16.
 */
public class AggStatsInputs {

    public final Optional<Device.Color> senseColorOptional;
    public final Optional<Calibration> calibrationOptional;
    public final ImmutableList<DeviceData> deviceDataList;
    public final ImmutableList<TrackerMotion> pillDataList;


    private AggStatsInputs(final Optional<Device.Color> senseColorOptional,
                          final Optional<Calibration> calibrationOptional,
                          final ImmutableList<DeviceData> deviceDataList,
                          final ImmutableList<TrackerMotion> pillDataList) {
        this.senseColorOptional = senseColorOptional;
        this.calibrationOptional = calibrationOptional;
        this.deviceDataList = deviceDataList;
        this.pillDataList = pillDataList;
    }

    public static AggStatsInputs create(final Optional<Device.Color> senseColorOptional,
                                 final Optional<Calibration> calibrationOptional,
                                 final Response<ImmutableList<DeviceData>> deviceDataListResponse,
                                 final ImmutableList<TrackerMotion> pillDataList) {
        return new AggStatsInputs(
                senseColorOptional,
                calibrationOptional,
                deviceDataListResponse.data,
                pillDataList);
    }
}

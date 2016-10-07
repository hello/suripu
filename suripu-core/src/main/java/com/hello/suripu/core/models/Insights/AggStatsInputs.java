package com.hello.suripu.core.models.Insights;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.pill.data.AggStatTrackerMotion;
import com.hello.suripu.core.sense.data.AggStatDeviceData;

import java.util.List;

/**
 * Created by jyfan on 7/25/16.
 */
public class AggStatsInputs {

    public final Optional<Device.Color> senseColorOptional;
    public final Optional<Calibration> calibrationOptional;
    public final ImmutableList<AggStatDeviceData> aggStatDeviceDataList;
    public final ImmutableList<AggStatTrackerMotion> aggStatPillDataList;


    private AggStatsInputs(final Optional<Device.Color> senseColorOptional,
                          final Optional<Calibration> calibrationOptional,
                          final ImmutableList<AggStatDeviceData> aggStatDeviceDataList,
                          final ImmutableList<AggStatTrackerMotion> aggStatPillDataList) {
        this.senseColorOptional = senseColorOptional;
        this.calibrationOptional = calibrationOptional;
        this.aggStatDeviceDataList = aggStatDeviceDataList;
        this.aggStatPillDataList = aggStatPillDataList;
    }

    public static AggStatsInputs create(final Optional<Device.Color> senseColorOptional,
                                 final Optional<Calibration> calibrationOptional,
                                 final Response<ImmutableList<DeviceData>> deviceDataListResponse,
                                 final ImmutableList<TrackerMotion> pillDataList) {

        final List<AggStatDeviceData> aggStatDeviceDatas = AggStatDeviceData.createAggStatDeviceDataList(deviceDataListResponse.data);

        final List<AggStatTrackerMotion> aggStatTrackerMotions = AggStatTrackerMotion.createAggStatTrackerMotionList(pillDataList);

        return new AggStatsInputs(
                senseColorOptional,
                calibrationOptional,
                ImmutableList.copyOf(aggStatDeviceDatas),
                ImmutableList.copyOf(aggStatTrackerMotions));
    }

    public static AggStatsInputs create(final Optional<Device.Color> senseColorOptional,
                                        final Optional<Calibration> calibrationOptional,
                                        final List<AggStatDeviceData> aggStatsDeviceDataList,
                                        final List<AggStatTrackerMotion> pillDataList) {
        return new AggStatsInputs(
                senseColorOptional,
                calibrationOptional,
                ImmutableList.copyOf(aggStatsDeviceDataList),
                ImmutableList.copyOf(pillDataList));
    }

}

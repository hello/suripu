package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.AggStatsInputs;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 7/14/16.
 */
public class AggStatsComputerTest {

    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final Long FAKE_DEVICE_ID = 9998L;
    private final String FAKE_EXTERNAL_ID = "fakeit";
    private final DeviceId FAKE_DEVICE_ID_EXT = DeviceId.create(FAKE_EXTERNAL_ID);

    private final List<DeviceData> FAKE_DEVICE_DATAS = Lists.newArrayList(
            DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, 1, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8),
            DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, 1, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8),
            DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, 1, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8));

    @Test
    public void computeAggStats_noData() {
        final DateTime dateLocal = DateTime.now();

        final ImmutableList<DeviceData> deviceDataList = ImmutableList.copyOf(Lists.<DeviceData>newArrayList());
        final ImmutableList<TrackerMotion> trackerMotionList = ImmutableList.copyOf(Lists.<TrackerMotion>newArrayList());

        final AggStatsInputs aggStatsInputs = AggStatsInputs.create(Optional.<Device.Color>absent(),
                                                                    Optional.<Calibration>absent(),
                                                                    new Response(deviceDataList, Response.Status.SUCCESS, Optional.<Exception>absent()),
                                                                    trackerMotionList);

        final Optional<AggStats> aggStats = AggStatsComputer.computeAggStats(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID_EXT, dateLocal, aggStatsInputs);
        assertThat(aggStats.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void computeAggStats() {
        final DateTime dateLocal = DateTime.now();

        final ImmutableList<DeviceData> deviceDataList = ImmutableList.copyOf(FAKE_DEVICE_DATAS);
        final ImmutableList<TrackerMotion> trackerMotionList = ImmutableList.copyOf(Lists.<TrackerMotion>newArrayList());

        final AggStatsInputs aggStatsInputs = AggStatsInputs.create(Optional.<Device.Color>absent(),
                Optional.<Calibration>absent(),
                new Response(deviceDataList, Response.Status.SUCCESS, Optional.<Exception>absent()),
                trackerMotionList);

        final Optional<AggStats> aggStats = AggStatsComputer.computeAggStats(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID_EXT, dateLocal, aggStatsInputs);
        assertThat(aggStats.isPresent(), is(Boolean.TRUE));
        assertThat(aggStats.get().accountId, is(FAKE_ACCOUNT_ID));

    }
}

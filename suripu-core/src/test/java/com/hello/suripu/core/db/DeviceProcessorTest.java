package com.hello.suripu.core.db;


import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.models.device.v2.DeviceProcessor;
import com.hello.suripu.core.models.device.v2.Pill;
import com.hello.suripu.core.models.device.v2.Sense;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeat;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;
import com.hello.suripu.core.util.PillColorUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeviceProcessorTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceProcessorTest.class);
    private DeviceProcessor deviceProcessor;

    private final DeviceAccountPair senseAccountPairLastSeen = new DeviceAccountPair(123L, 9L, "sense9", new DateTime(1431457194000L));
    private final DeviceAccountPair senseAccountPairLastHour = new DeviceAccountPair(123L, 1L, "sense1", new DateTime(1431457194000L));
    private final DeviceAccountPair senseAccountPairLastWeek = new DeviceAccountPair(123L, 2L, "sense2", new DateTime(1431457194000L));
    private final DeviceStatus senseStatusLastSeen = new DeviceStatus(9L, 9L, "sense-fw-9", 0, new DateTime(1431457194000L), 60000);
    private final DeviceStatus senseStatusLastHour = new DeviceStatus(1L, 1L, "sense-fw-1", 0, new DateTime(1431457194000L), 60000);
    private final DeviceStatus senseStatusLastWeek = new DeviceStatus(2L, 2L, "sense-fw-2", 0, new DateTime(1431457194000L), 60000);
    private final DeviceAccountPair pillAccountPairHeartbeat = new DeviceAccountPair(123L, 1L, "pill1", new DateTime(1431457194000L));

    private final PillHeartBeat pillHeartBeat = PillHeartBeat.create("abc", 1, 70, 60000, new DateTime(1431457194000L));
    private final DeviceAccountPair senseAccountPairSenseColor = new DeviceAccountPair(123L, 3L, "sense3", new DateTime(1431457194000L));
    private final DeviceAccountPair pillAccountPairTrackerMotion = new DeviceAccountPair(123L, 2L, "pill2", new DateTime(1431457194000L));
    private final DeviceStatus pillStatusTrackerMotion = new DeviceStatus(2L, 2L, "3", 90, new DateTime(1431457194000L), 60000);


    private final DeviceAccountPair senseAccountPairWifi = new DeviceAccountPair(123L, 4L, "sense4", new DateTime(1431457194000L));
    private final DeviceAccountPair senseAccountPairPillColor = new DeviceAccountPair(123L, 4L, "sense4", new DateTime(1431457194000L));
    private final DeviceAccountPair senseAccountPairNoStatus = new DeviceAccountPair(666L, 77L, "sense88", new DateTime(1431457194000L));
    private final DeviceAccountPair pillAccountPairNoStatus = new DeviceAccountPair(555L, 88L, "pill99", new DateTime(1431457194000L));
    private final PillHeartBeat pillHeartBeatNoStatus = PillHeartBeat.create("abc", 2, 88, 60000, new DateTime(1431457194000L));

    private final WifiInfo expectedWifiInfo = WifiInfo.create(senseAccountPairWifi.externalDeviceId, "hello", -98, new DateTime(1431457194000L));



    @Before
    public void setUp(){
        final DeviceDAO deviceDAO = mock(DeviceDAO.class);
        when(deviceDAO.getSensesForAccountId(senseAccountPairWifi.accountId)).thenReturn(ImmutableList.copyOf(Arrays.asList(senseAccountPairWifi)));

        final PillHeartBeatDAODynamoDB pillHeartBeatDAO = mock(PillHeartBeatDAODynamoDB.class);
        when(pillHeartBeatDAO.get(pillAccountPairHeartbeat.externalDeviceId)).thenReturn(Optional.of(pillHeartBeat));
        when(pillHeartBeatDAO.get(pillAccountPairTrackerMotion.externalDeviceId)).thenReturn(Optional.<PillHeartBeat>absent());
        when(pillHeartBeatDAO.get(pillHeartBeatNoStatus.pillId)).thenReturn(Optional.<PillHeartBeat>absent());

        final TrackerMotionDAO trackerMotionDAO = mock(TrackerMotionDAO.class);
        when(trackerMotionDAO.pillStatus(pillAccountPairTrackerMotion.internalDeviceId, pillAccountPairTrackerMotion.accountId)).thenReturn(Optional.of(pillStatusTrackerMotion));
        when(trackerMotionDAO.pillStatus(pillAccountPairNoStatus.internalDeviceId, pillAccountPairNoStatus.accountId)).thenReturn(Optional.<DeviceStatus>absent());

        final SenseColorDAO senseColorDAO = mock(SenseColorDAO.class);
        when(senseColorDAO.get(senseAccountPairSenseColor.externalDeviceId)).thenReturn(Optional.of(Sense.Color.BLACK));

        final String testPillId = "pill3";
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = mock(MergedUserInfoDynamoDB.class);
        final OutputProtos.SyncResponse.PillSettings pillSettings = OutputProtos.SyncResponse.PillSettings.newBuilder()
                .setPillColor(PillColorUtil.argbToIntBasedOnSystemEndianess(PillColorUtil.colorToARGB(new Color(0xFE, 0x00, 0x00)))) // Red
                .setPillId(testPillId).build();
        when(mergedUserInfoDynamoDB.getInfo(senseAccountPairPillColor.externalDeviceId, senseAccountPairPillColor.accountId)).thenReturn(Optional.of(new UserInfo(
                        senseAccountPairPillColor.externalDeviceId,
                        senseAccountPairLastHour.accountId,
                        Collections.<Alarm>emptyList(),
                        Optional.of(RingTime.createEmpty()),
                        Optional.<DateTimeZone>absent(),
                        Optional.of(pillSettings),
                        0L)
        ));

        final WifiInfoDAO wifiInfoDAO  = mock(WifiInfoDAO.class);
        when(wifiInfoDAO.getBatchStrict(Arrays.asList(senseAccountPairWifi.externalDeviceId))).thenReturn(ImmutableMap.of(senseAccountPairWifi.externalDeviceId, Optional.of(expectedWifiInfo)));

        final SensorsViewsDynamoDB sensorsViewsDynamoDB = mock(SensorsViewsDynamoDB.class);
        when(sensorsViewsDynamoDB.senseStatus(senseAccountPairLastSeen.externalDeviceId, senseAccountPairLastSeen.accountId, senseAccountPairLastSeen.internalDeviceId))
                .thenReturn(Optional.of(senseStatusLastSeen));

        deviceProcessor = new DeviceProcessor.Builder()
                .withDeviceDAO(deviceDAO)
                .withMergedUserInfoDynamoDB(mergedUserInfoDynamoDB)
                .withPillHeartbeatDAO(pillHeartBeatDAO)
                .withSenseColorDAO(senseColorDAO)
                .withTrackerMotionDAO(trackerMotionDAO)
                .withWifiInfoDAO(wifiInfoDAO)
                .withSensorsViewDynamoDB(sensorsViewsDynamoDB)
                .build();
    }

    @Test
    public void testGetWifiInfo() {
        final Map<String, Optional<WifiInfo>> wifiInfoMapOptional = deviceProcessor.retrieveWifiInfoMap(Arrays.asList(senseAccountPairWifi));
        assertThat(wifiInfoMapOptional.isEmpty(), is(false));
        assertThat(wifiInfoMapOptional.get(senseAccountPairWifi.externalDeviceId).isPresent(), is(true));
        final WifiInfo wifiInfo = wifiInfoMapOptional.get(senseAccountPairWifi.externalDeviceId).get();
        assertThat(wifiInfo, equalTo(expectedWifiInfo));
    }

    @Test
    public void testGetPillStatusHeartbeat() {
        final Optional<PillHeartBeat> pillHeartBeatOptional = deviceProcessor.retrievePillHeartBeat(pillAccountPairHeartbeat);
        assertThat(pillHeartBeatOptional.isPresent(), is(true));
        assertThat(pillHeartBeatOptional.get(), equalTo(pillHeartBeat));
    }

    @Test
    public void testGetPillStatusTrackerMotion() {
        final Optional<PillHeartBeat> pillHeartBeatOptional = deviceProcessor.retrievePillHeartBeat(pillAccountPairTrackerMotion);
        assertThat(pillHeartBeatOptional.isPresent(), is(true));
//        assertThat(pillHeartBeatOptional.get(), equalTo(pillHeartBeatTrackerMotion));
    }

    // TODO: REFACTOR THIS (Tim)
//    @Test(expected = NullPointerException.class)
//    public void testGetPillStatusDataUnavailable() {
//        final Optional<PillHeartBeat> deviceStatusOptional = deviceProcessor.retrievePillHeartBeat(pillAccountPairNoStatus);
////        assertThat(deviceStatusOptional.isPresent(), is(false));
//    }

    @Test
    public void testGetPillColor() {
        final Optional<Pill.Color> pillColorOptional = deviceProcessor.retrievePillColor(senseAccountPairPillColor.accountId, Arrays.asList(senseAccountPairPillColor));
        assertThat(pillColorOptional.isPresent(), is(true));
        assertThat(pillColorOptional.get(), equalTo(Pill.Color.RED));
    }
}

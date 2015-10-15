package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.flipper.GroupFlipper;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.service.SignedMessage;
import com.hello.suripu.service.configuration.OTAConfiguration;
import com.librato.rollout.RolloutClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;


/**
 * Created by jnorgan on 10/14/15.
 */

public class ReceiveResourceIT extends ResourceTest {
    private static final String SENSE_ID = "fake-sense";
    private static final byte[] KEY = "1234567891234567".getBytes();
    private static final Integer FIRMWARE_VERSION = 12345678;
    private static final Integer FW_VERSION_0_9_22_RC7 = 1530439804;
    private static final Integer FUTURE_UNIX_TIMESTAMP = 2139176514; //14 Oct 2037 23:41:54 GMT
    private List<UserInfo> userInfoList;
    private List<OutputProtos.SyncResponse.FileDownload> fileList;
    private DateTimeZone userTimeZone;
    private ReceiveResource receiveResource;


    @Before
    public void setUp() {
        super.setUp();

        BaseResourceTestHelper.kinesisLoggerFactoryStubGet(kinesisLoggerFactory, QueueName.LOGS, dataLogger);
        BaseResourceTestHelper.kinesisLoggerFactoryStubGet(kinesisLoggerFactory, QueueName.SENSE_SENSORS_DATA, dataLogger);

        final ReceiveResource receiveResource = new ReceiveResource(
                keyStore,
                kinesisLoggerFactory,
                mergedUserInfoDynamoDB,
                ringTimeHistoryDAODynamoDB,
                true,
                firmwareUpdateStore,
                groupFlipper,
                senseUploadConfiguration,
                otaConfiguration,
                responseCommandsDAODynamoDB,
                240,
                calibrationDAO
        );
        receiveResource.request = httpServletRequest;
        receiveResource.featureFlipper = featureFlipper;
        this.receiveResource = spy(receiveResource);

        BaseResourceTestHelper.stubGetHeader(receiveResource.request, "X-Forwarded-For", "127.0.0.1");
        final List<Alarm> alarmList = Lists.newArrayList();
        userTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

        final UserInfo userInfo = new UserInfo(
                SENSE_ID,
                1234L,
                alarmList,
                Optional.<RingTime>absent(),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                12345678L);
        userInfoList = Lists.newArrayList(userInfo);

        final OutputProtos.SyncResponse.FileDownload fileDownload = OutputProtos.SyncResponse.FileDownload.newBuilder()
                .setHost("test")
                .setCopyToSerialFlash(true)
                .setResetApplicationProcessor(false)
                .setSerialFlashFilename("mcuimgx.bin")
                .setSerialFlashPath("/sys/")
                .setSdCardFilename("mcuimgx.bin")
                .setSdCardPath("/")
                .build();
        fileList = Lists.newArrayList(fileDownload);
    }

    @Test
    public void testOTAPublicRelease() {
        BaseResourceTestHelper.stubGetClientDetails(oAuthTokenStore, Optional.of(BaseResourceTestHelper.getAccessToken()));
        BaseResourceTestHelper.stubKeyFromKeyStore(keyStore, SENSE_ID, Optional.of(KEY));
        BaseResourceTestHelper.stubGetHeader(httpServletRequest, HelloHttpHeader.SENSE_ID, SENSE_ID);
        stubGetUserInfo(mergedUserInfoDynamoDB, userInfoList);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.ENABLE_OTA_UPDATES, Collections.EMPTY_LIST, true);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.OTA_RELEASE, Collections.EMPTY_LIST, true);
        stubGetOTAWindowStart(otaConfiguration, 11);
        stubGetOTAWindowEnd(otaConfiguration, 20);
        stubGetPopulatedFirmwareFileListForGroup(firmwareUpdateStore, FeatureFlipper.OTA_RELEASE, FIRMWARE_VERSION, fileList);

        final long unixTime = DateTime.now().withZone(userTimeZone).getMillis() / 1000L;

        final byte[] data = receiveResource.receiveBatchSenseData(generateValidProtobufWithSignature(KEY, 3600, FIRMWARE_VERSION, (int)unixTime));

        final byte[] protobufBytes = Arrays.copyOfRange(data, 16 + 32, data.length);
        final OutputProtos.SyncResponse syncResponse;
        try {
            syncResponse = OutputProtos.SyncResponse.parseFrom(protobufBytes);
            assertThat(syncResponse.hasResetMcu(), is(true));
            assertThat(syncResponse.getResetMcu(), is(false));
            assertThat(syncResponse.getFilesCount(), is(1));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }

    @Test
    public void testOTAPublicReleaseOutsideWindow() {
        BaseResourceTestHelper.stubGetClientDetails(oAuthTokenStore, Optional.of(BaseResourceTestHelper.getAccessToken()));
        BaseResourceTestHelper.stubKeyFromKeyStore(keyStore, SENSE_ID, Optional.of(KEY));
        BaseResourceTestHelper.stubGetHeader(httpServletRequest, HelloHttpHeader.SENSE_ID, SENSE_ID);
        stubGetUserInfo(mergedUserInfoDynamoDB, userInfoList);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.ENABLE_OTA_UPDATES, Collections.EMPTY_LIST, true);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.OTA_RELEASE, Collections.EMPTY_LIST, true);
        stubGetOTAWindowStart(otaConfiguration, 23);
        stubGetOTAWindowEnd(otaConfiguration, 23);
        stubGetPopulatedFirmwareFileListForGroup(firmwareUpdateStore, FeatureFlipper.OTA_RELEASE, FIRMWARE_VERSION, fileList);

        final long unixTime = DateTime.now().withZone(userTimeZone).getMillis() / 1000L;

        final byte[] data = receiveResource.receiveBatchSenseData(generateValidProtobufWithSignature(KEY, 3600, FIRMWARE_VERSION, (int)unixTime));

        final byte[] protobufBytes = Arrays.copyOfRange(data, 16 + 32, data.length);
        final OutputProtos.SyncResponse syncResponse;
        try {
            syncResponse = OutputProtos.SyncResponse.parseFrom(protobufBytes);
            assertThat(syncResponse.hasResetMcu(), is(false));
            assertThat(syncResponse.getFilesCount(), is(0));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }

    @Test
    public void testOTAForFactoryClockSyncIssueNoPillsHighUptime() {
        BaseResourceTestHelper.stubGetClientDetails(oAuthTokenStore, Optional.of(BaseResourceTestHelper.getAccessToken()));
        BaseResourceTestHelper.stubKeyFromKeyStore(keyStore, SENSE_ID, Optional.of(KEY));
        BaseResourceTestHelper.stubGetHeader(httpServletRequest, HelloHttpHeader.SENSE_ID, SENSE_ID);
        stubGetUserInfo(mergedUserInfoDynamoDB, userInfoList);

        stubGetOTAWindowStart(otaConfiguration, 11);
        stubGetOTAWindowEnd(otaConfiguration, 20);
        stubGetPopulatedFirmwareFileListForGroup(firmwareUpdateStore, FeatureFlipper.OTA_RELEASE, FW_VERSION_0_9_22_RC7, fileList);

        final List<String> groups = Lists.newArrayList(FeatureFlipper.OTA_RELEASE);
        stubGetGroups(groupFlipper, groups);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.ENABLE_OTA_UPDATES, groups, true);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.OTA_RELEASE, groups, true);

        final byte[] data = receiveResource.receiveBatchSenseData(generateValidProtobufWithSignature(KEY, 3600, FW_VERSION_0_9_22_RC7, FUTURE_UNIX_TIMESTAMP));

        final byte[] protobufBytes = Arrays.copyOfRange(data, 16 + 32, data.length);
        final OutputProtos.SyncResponse syncResponse;
        try {
            syncResponse = OutputProtos.SyncResponse.parseFrom(protobufBytes);
            assertThat(syncResponse.hasResetMcu(), is(true));
            assertThat(syncResponse.getResetMcu(), is(false));
            assertThat(syncResponse.getFilesCount(), is(1));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }

    @Test
    public void testOTAForFactoryClockSyncIssueNoPillsLowUptime() {
        BaseResourceTestHelper.stubGetClientDetails(oAuthTokenStore, Optional.of(BaseResourceTestHelper.getAccessToken()));
        BaseResourceTestHelper.stubKeyFromKeyStore(keyStore, SENSE_ID, Optional.of(KEY));
        BaseResourceTestHelper.stubGetHeader(httpServletRequest, HelloHttpHeader.SENSE_ID, SENSE_ID);
        stubGetUserInfo(mergedUserInfoDynamoDB, userInfoList);

        stubGetOTAWindowStart(otaConfiguration, 11);
        stubGetOTAWindowEnd(otaConfiguration, 20);
        stubGetPopulatedFirmwareFileListForGroup(firmwareUpdateStore, FeatureFlipper.OTA_RELEASE, FW_VERSION_0_9_22_RC7, fileList);

        final List<String> groups = Lists.newArrayList(FeatureFlipper.OTA_RELEASE);
        stubGetGroups(groupFlipper, groups);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.ENABLE_OTA_UPDATES, groups, true);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.OTA_RELEASE, groups, true);

        final byte[] data = receiveResource.receiveBatchSenseData(generateValidProtobufWithSignature(KEY, 899, FW_VERSION_0_9_22_RC7, FUTURE_UNIX_TIMESTAMP));

        final byte[] protobufBytes = Arrays.copyOfRange(data, 16 + 32, data.length);
        final OutputProtos.SyncResponse syncResponse;
        try {
            syncResponse = OutputProtos.SyncResponse.parseFrom(protobufBytes);
            assertThat(syncResponse.hasResetMcu(), is(false));
            assertThat(syncResponse.getFilesCount(), is(0));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }

    @Test
    public void testOTAForFactoryClockSyncIssueTwoPillsLowUptime() {
        BaseResourceTestHelper.stubGetClientDetails(oAuthTokenStore, Optional.of(BaseResourceTestHelper.getAccessToken()));
        BaseResourceTestHelper.stubKeyFromKeyStore(keyStore, SENSE_ID, Optional.of(KEY));
        BaseResourceTestHelper.stubGetHeader(httpServletRequest, HelloHttpHeader.SENSE_ID, SENSE_ID);

        final List<Alarm> alarmList = Lists.newArrayList();
        final OutputProtos.SyncResponse.PillSettings pillSettings = OutputProtos.SyncResponse.PillSettings.newBuilder()
                .setPillId("fake-pill")
                .setPillColor(1)
                .build();
        final UserInfo userInfo = new UserInfo(
                SENSE_ID,
                1234L,
                alarmList,
                Optional.<RingTime>absent(),
                Optional.of(userTimeZone),
                Optional.of(pillSettings),
                12345678L);
        final List<UserInfo> userInfoListPills = Lists.newArrayList(userInfo, userInfo);

        stubGetUserInfo(mergedUserInfoDynamoDB, userInfoListPills);

        stubGetOTAWindowStart(otaConfiguration, 11);
        stubGetOTAWindowEnd(otaConfiguration, 20);
        stubGetPopulatedFirmwareFileListForGroup(firmwareUpdateStore, FeatureFlipper.OTA_RELEASE, FW_VERSION_0_9_22_RC7, fileList);

        final List<String> groups = Lists.newArrayList(FeatureFlipper.OTA_RELEASE);
        stubGetGroups(groupFlipper, groups);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.ENABLE_OTA_UPDATES, groups, true);
        stubGetFeatureActive(featureFlipper, FeatureFlipper.OTA_RELEASE, groups, true);

        final byte[] data = receiveResource.receiveBatchSenseData(generateValidProtobufWithSignature(KEY, 899, FW_VERSION_0_9_22_RC7, FUTURE_UNIX_TIMESTAMP));

        final byte[] protobufBytes = Arrays.copyOfRange(data, 16 + 32, data.length);
        final OutputProtos.SyncResponse syncResponse;
        try {
            syncResponse = OutputProtos.SyncResponse.parseFrom(protobufBytes);
            assertThat(syncResponse.hasResetMcu(), is(true));
            assertThat(syncResponse.getResetMcu(), is(false));
            assertThat(syncResponse.getFilesCount(), is(1));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }


    private byte[] generateValidProtobufWithSignature(final byte[] key, final Integer uptime, final Integer firmwareVersion, final Integer unixTime){

        final DataInputProtos.periodic_data data = DataInputProtos.periodic_data.newBuilder()
                .setUnixTime(unixTime)
                .build();
        final DataInputProtos.batched_periodic_data batch = DataInputProtos.batched_periodic_data.newBuilder()
                .setDeviceId(SENSE_ID)
                .setFirmwareVersion(firmwareVersion)
                .setUptimeInSecond(uptime)
                .addData(data)
                .build();

        final byte[] body  = batch.toByteArray();
        final Optional<byte[]> signedOptional = SignedMessage.sign(body, key);
        assertThat(signedOptional.isPresent(), is(true));
        final byte[] signed = signedOptional.get();
        final byte[] iv = Arrays.copyOfRange(signed, 0, 16);
        final byte[] sig = Arrays.copyOfRange(signed, 16, 16 + 32);
        final byte[] message = new byte[signed.length];
        copyTo(message, body, 0, body.length);
        copyTo(message, iv, body.length, body.length + iv.length);
        copyTo(message, sig, body.length + iv.length, message.length);
        return message;

    }

    private void copyTo(final byte[] dest, final byte[] src, final int start, final int end){
        for(int i = start; i < end; i++){
            dest[i] = src[i-start];
        }

    }

    private void stubGetUserInfo (final MergedUserInfoDynamoDB mergedUserInfoDynamoDB, final List<UserInfo> returnInfoList) {
        doReturn(returnInfoList).when(mergedUserInfoDynamoDB).getInfo(SENSE_ID);
    }

    private void stubGetFeatureActive (final RolloutClient featureFlipper, final String featureName, final List<String> groups, final Boolean returnValue) {
        doReturn(returnValue).when(featureFlipper).deviceFeatureActive(featureName, SENSE_ID, groups);
    }

    private void stubGetOTAWindowStart (final OTAConfiguration otaConfiguration, final Integer hourOfDay) {
        doReturn(hourOfDay).when(otaConfiguration).getStartUpdateWindowHour();
    }

    private void stubGetOTAWindowEnd (final OTAConfiguration otaConfiguration, final Integer hourOfDay) {
        doReturn(hourOfDay).when(otaConfiguration).getEndUpdateWindowHour();
    }

    private void stubGetPopulatedFirmwareFileListForGroup (final FirmwareUpdateStore firmwareUpdateStore, final String groupName, final Integer firmwareVersion, final List<OutputProtos.SyncResponse.FileDownload> fileList) {
        doReturn(fileList).when(firmwareUpdateStore).getFirmwareUpdate(SENSE_ID, groupName, firmwareVersion, false);
    }

    private void stubGetGroups (final GroupFlipper groupFlipper, final List<String> groups) {
        doReturn(groups).when(groupFlipper).getGroups(SENSE_ID);
    }
}

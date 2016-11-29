package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.util.PillColorUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 9/26/14.
 */
public class MergedUserInfoDynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(AlarmDAODynamoDB.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final String tableName = "alarm_info_test";
    private final String deviceId = "test_morpheus";
    private final long accountId = 1L;

    @Before
    public void setUp(){

        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            MergedUserInfoDynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(
                    this.amazonDynamoDBClient,
                    tableName
            );


        }catch (ResourceInUseException rie){
            LOGGER.warn("Can not create existing table");
        }

    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }

    @Test
    public void testCreateAndRetrieve(){
        this.mergedUserInfoDynamoDB.setTimeZone(this.deviceId, this.accountId, DateTimeZone.UTC);
        final Optional<UserInfo> retrieved = this.mergedUserInfoDynamoDB.getInfo(this.deviceId, this.accountId);
        assertThat(retrieved.isPresent(), is(true));
        assertThat(retrieved.get().timeZone.isPresent(), is(true));
    }

    @Test
    public void testUpdateNotAppend(){
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime previousRing = now.plusHours(1);
        final RingTime ringTime = new RingTime(previousRing.getMillis(), previousRing.getMillis(), new long[]{1L}, false);
        final List<Alarm> alarmListPrevious = new ArrayList<>();
        final List<Alarm> alarmListCurrent = new ArrayList<>();
        alarmListCurrent.add(new Alarm(previousRing.minusMinutes(5).getYear(), previousRing.minusMinutes(5).getMonthOfYear(), previousRing.minusMinutes(5).getDayOfMonth(), previousRing.minusMinutes(5).getHourOfDay(), previousRing.minusMinutes(5).getMinuteOfHour(), new HashSet<Integer>(Arrays.asList(previousRing.minusMinutes(5).getDayOfWeek())),
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));
        this.mergedUserInfoDynamoDB.createUserInfoWithEmptyAlarmList(this.deviceId, this.accountId, DateTimeZone.UTC);
        this.mergedUserInfoDynamoDB.setTimeZone(this.deviceId, this.accountId, DateTimeZone.UTC);  // Timezone must set first, or ringtime will be reset
        this.mergedUserInfoDynamoDB.setRingTime(this.deviceId, this.accountId, ringTime);
        this.mergedUserInfoDynamoDB.setPillColor(this.deviceId, this.accountId, "Pang's Pill", new Color(0xFE, 0x00, 0x00));
        this.mergedUserInfoDynamoDB.setAlarms(deviceId, accountId, DateTime.now(DateTimeZone.UTC).minusHours(1).getMillis(), alarmListPrevious, alarmListCurrent, DateTimeZone.UTC);

        final List<UserInfo> userInfoList = this.mergedUserInfoDynamoDB.getInfo(this.deviceId);
        assertThat(userInfoList.size(), is(1));
        assertThat(userInfoList.get(0).ringTime.isPresent(), is(true));
        assertThat(userInfoList.get(0).timeZone.isPresent(), is(true));
        assertThat(userInfoList.get(0).ringTime.get().equals(ringTime), is(true));
        assertThat(userInfoList.get(0).timeZone.get().equals(DateTimeZone.UTC), is(true));

        final int intARGB = userInfoList.get(0).pillColor.get().getPillColor();

        if(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            assertThat(intARGB, is(0x0000FEFF));  // WTF? Now mac is little endian??!?!?!
        }else{
            assertThat(intARGB, is(0xFFFE0000));
        }
    }

    @Test
    public void testGetFromNoneExistDevice(){

        final List<UserInfo> userInfoList = this.mergedUserInfoDynamoDB.getInfo("fitbit");
        assertThat(userInfoList.size(), is(0));

        assertThat(this.mergedUserInfoDynamoDB.getInfo("fitbit", 1L).isPresent(), is(false));
    }

    @Test
    public void testUnlinkAccountAndDeviceId(){
        final String deviceId = "Pang's Morpheus";
        this.mergedUserInfoDynamoDB.setTimeZone(deviceId, 0L, DateTimeZone.UTC);
        this.mergedUserInfoDynamoDB.setTimeZone(deviceId, 1L, DateTimeZone.UTC);
        final Optional<UserInfo> deleted = this.mergedUserInfoDynamoDB.unlinkAccountToDevice(0L, deviceId);
        assertThat(deleted.isPresent(), is(true));
        assertThat(deleted.get().deviceId, is(deviceId));


        final List<UserInfo> existingPairs = this.mergedUserInfoDynamoDB.getInfo(deviceId);
        assertThat(existingPairs.size(), is(1));
        assertThat(existingPairs.get(0).deviceId, is(deviceId));
        assertThat(existingPairs.get(0).accountId, is(1L));

        final Optional<UserInfo> deleteNoExist = this.mergedUserInfoDynamoDB.unlinkAccountToDevice(911L, deviceId);
        assertThat(deleteNoExist.isPresent(), is(false));
    }


    @Test
    public void testDeletePillColor(){
        final String senseId = "Pang's Sense";
        final String pillId = "Pang's Pill";
        this.mergedUserInfoDynamoDB.setTimeZone(senseId, 1L, DateTimeZone.getDefault());
        this.mergedUserInfoDynamoDB.setPillColor(senseId, 1L, pillId, Color.red);

        final Optional<UserInfo> userInfoOptional =  this.mergedUserInfoDynamoDB.getInfo(senseId, 1L);
        assertThat(userInfoOptional.isPresent(), is(true));

        if(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            assertThat(userInfoOptional.get().pillColor.get().getPillColor(), is(0x0000FFFF));
        }else{
            assertThat(userInfoOptional.get().pillColor.get().getPillColor(), is(0xFFFF0000));
        }

        this.mergedUserInfoDynamoDB.deletePillColor(senseId, 1L, pillId);
        final Optional<UserInfo> userInfoNoPillColor = this.mergedUserInfoDynamoDB.getInfo(senseId, 1L);
        assertThat(userInfoNoPillColor.isPresent(), is(true));
        assertThat(userInfoNoPillColor.get().pillColor.isPresent(), is(false));
        assertThat(userInfoNoPillColor.get().timeZone.get(), is(DateTimeZone.getDefault()));
    }


    @Test
    public void testPillColorUpdateAfterReLink(){
        final String senseId = "Pang's Sense";
        final String pillId = "Pang's Pill";
        this.mergedUserInfoDynamoDB.setTimeZone(senseId, 1L, DateTimeZone.getDefault());
        final Optional<Color> firstColor = this.mergedUserInfoDynamoDB.setNextPillColor(senseId, 1L, pillId);
        assertThat(firstColor.isPresent(), is(true));
        assertThat(firstColor.get(), is(PillColorUtil.BLUE));

        final String pillId2 = "Pang's Pill 2";
        this.mergedUserInfoDynamoDB.setTimeZone(senseId, 2L, DateTimeZone.getDefault());
        final Optional<Color> secondColor = this.mergedUserInfoDynamoDB.setNextPillColor(senseId, 2L, pillId2);
        assertThat(secondColor.isPresent(), is(true));
        assertThat(secondColor.get(), is(PillColorUtil.RED));

        this.mergedUserInfoDynamoDB.unlinkAccountToDevice(1L, senseId);
        this.mergedUserInfoDynamoDB.setTimeZone(senseId, 1L, DateTimeZone.getDefault());
        final Optional<Color> reRegisterColor = this.mergedUserInfoDynamoDB.setNextPillColor(senseId, 1L, pillId);
        assertThat(reRegisterColor.isPresent(), is(true));
        assertThat(reRegisterColor.get(), is(PillColorUtil.BLUE));
    }


    @Test
    public void testUpdateConflict(){
        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        items.put(MergedUserInfoDynamoDB.UPDATED_AT_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(DateTime.now(DateTimeZone.UTC).plusHours(1).getMillis()))));

        final HashMap<String, AttributeValue> keys = new HashMap<>();
        keys.put(MergedUserInfoDynamoDB.MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        keys.put(MergedUserInfoDynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));


        final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(this.tableName)
                .withKey(keys)
                .withAttributeUpdates(items)
                .withReturnValues(ReturnValue.ALL_NEW);
        this.amazonDynamoDBClient.updateItem(updateItemRequest);
        final boolean updated = this.mergedUserInfoDynamoDB.setAlarms(deviceId, accountId, DateTime.now(DateTimeZone.UTC).getMillis(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, DateTimeZone.UTC);
        assertThat(updated, is(false));
    }

    private Alarm nonRepeatedFromRingTime(final DateTime ringTime, final boolean smart){
        return new Alarm(ringTime.getYear(),
                ringTime.getMonthOfYear(),
                ringTime.getDayOfMonth(),
                ringTime.getHourOfDay(),
                ringTime.getMinuteOfHour(),
                new HashSet<Integer>(), false, true, true, smart, new AlarmSound(0, "Pluse"), "id");
    }

    @Test
    public void testUpdateAlarmShouldUpdateRingTime(){
        final String senseId = "Sense";
        final long accountId  = 1;

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime previousRing = now.plusHours(1);
        this.mergedUserInfoDynamoDB.createUserInfoWithEmptyAlarmList(senseId,accountId, DateTimeZone.UTC);

        this.mergedUserInfoDynamoDB.setTimeZone(senseId, accountId, DateTimeZone.UTC);
        this.mergedUserInfoDynamoDB.setRingTime(senseId, accountId, new RingTime(previousRing.minusMinutes(5).getMillis(),
                previousRing.getMillis(), new long[0], true, Lists.newArrayList()));

        final UserInfo userInfo = this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get();
        assertThat(userInfo.ringTime.get().actualRingTimeUTC,
                is(previousRing.minusMinutes(5).getMillis()));
        assertThat(userInfo.ringTime.get().fromSmartAlarm,
                is(true));

        final ArrayList<Alarm> alarms = new ArrayList<>();
        final DateTime insertedAlarmRingTime = now.plusMinutes(10).withSecondOfMinute(0).withMillisOfSecond(0);

        alarms.add(nonRepeatedFromRingTime(insertedAlarmRingTime, false));
        final boolean updated = this.mergedUserInfoDynamoDB.setAlarms(senseId, accountId, userInfo.lastUpdatedAt, Collections.EMPTY_LIST, alarms, DateTimeZone.UTC);

        assertThat(updated, is(true));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().actualRingTimeUTC,
                is(insertedAlarmRingTime.getMillis()));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().fromSmartAlarm,
                is(false));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().alarmList.size() > 0,
                is(true));
    }


    @Test
    public void testUpdateTheSameAlarmShouldNotUpdateRingTime(){
        final String senseId = deviceId;
        final long accountId  = 1L;

        final DateTime now = DateTime.now(DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
        final DateTime previousRing = now.plusHours(1);
        this.mergedUserInfoDynamoDB.createUserInfoWithEmptyAlarmList(senseId,accountId, DateTimeZone.getDefault());
        this.mergedUserInfoDynamoDB.setTimeZone(senseId, accountId, DateTimeZone.getDefault());
        final List<Alarm> alarmListPrevious = new ArrayList<>();
        alarmListPrevious.add(new Alarm(previousRing.getYear(), previousRing.getMonthOfYear(), previousRing.getDayOfMonth(), previousRing.getHourOfDay(), previousRing.getMinuteOfHour(), new HashSet<Integer>(Arrays.asList(previousRing.getDayOfWeek())),
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));
        final List<Alarm> alarmListCurrent = new ArrayList<>();
        alarmListCurrent.add(new Alarm(previousRing.minusMinutes(5).getYear(), previousRing.minusMinutes(5).getMonthOfYear(), previousRing.minusMinutes(5).getDayOfMonth(), previousRing.minusMinutes(5).getHourOfDay(), previousRing.minusMinutes(5).getMinuteOfHour(), new HashSet<Integer>(Arrays.asList(previousRing.minusMinutes(5).getDayOfWeek())),
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));
        final boolean updateAlarmList = this.mergedUserInfoDynamoDB.setAlarms(senseId, accountId, DateTime.now(DateTimeZone.UTC).minusHours(1).getMillis(), alarmListPrevious, alarmListCurrent, DateTimeZone.getDefault());
        this.mergedUserInfoDynamoDB.setRingTime(senseId, accountId, new RingTime(previousRing.minusMinutes(5).getMillis(),
                previousRing.getMillis(), new long[0], true, Lists.newArrayList()));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().actualRingTimeUTC,
                is(previousRing.minusMinutes(5).getMillis()));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().fromSmartAlarm,
                is(true));

        final long lastUpdateAt = this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().lastUpdatedAt;

        final ArrayList<Alarm> alarms = new ArrayList<>();
        alarms.add(nonRepeatedFromRingTime(previousRing, true));
        final boolean updated = this.mergedUserInfoDynamoDB.setAlarms(senseId, accountId,lastUpdateAt, alarms, alarms, DateTimeZone.getDefault());

        assertThat(updated, is(true));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().actualRingTimeUTC,
                is(previousRing.minusMinutes(5).getMillis()));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().expectedRingTimeUTC,
                is(previousRing.getMillis()));

        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().fromSmartAlarm,
                is(true));
    }

    @Test
    public void testUpdateTimeZoneShouldDeleteWorkerRingTime(){
        final String senseId = "Sense";
        final long accountId  = 1;

        final DateTimeZone userTimeZone1 = DateTimeZone.forID("America/Los_Angeles");
        this.mergedUserInfoDynamoDB.createUserInfoWithEmptyAlarmList(senseId,accountId, DateTimeZone.getDefault());
        this.mergedUserInfoDynamoDB.setTimeZone(senseId, accountId, userTimeZone1);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime previousRing = now.plusHours(1);
        this.mergedUserInfoDynamoDB.setRingTime(senseId, accountId, new RingTime(previousRing.minusMinutes(5).getMillis(),
                previousRing.getMillis(), new long[0], true, Lists.newArrayList()));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().actualRingTimeUTC,
                is(previousRing.minusMinutes(5).getMillis()));
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().fromSmartAlarm,
                is(true));
        assertThat(this.mergedUserInfoDynamoDB.getTimezone(senseId, accountId).get(), is(userTimeZone1));

        final DateTimeZone userTimeZone2 = DateTimeZone.forID("Asia/Hong_Kong");
        this.mergedUserInfoDynamoDB.setTimeZone(senseId, accountId, userTimeZone2);
        assertThat(this.mergedUserInfoDynamoDB.getInfo(senseId, accountId).get().ringTime.get().isEmpty(), is(true));
        assertThat(this.mergedUserInfoDynamoDB.getTimezone(senseId, accountId).get(), is(userTimeZone2));
    }

}

package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

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
        this.mergedUserInfoDynamoDB.setAlarms(this.deviceId, this.accountId, Collections.EMPTY_LIST);
        final Optional<UserInfo> retrieved = this.mergedUserInfoDynamoDB.getInfo(this.deviceId, this.accountId);
        assertThat(retrieved.isPresent(), is(true));
        assertThat(retrieved.get().timeZone.isPresent(), is(false));
    }

    @Test
    public void testUpdateNotAppend(){
        final RingTime ringTime = new RingTime(DateTime.now().getMillis(), DateTime.now().getMillis(), new long[]{1L}, false);
        this.mergedUserInfoDynamoDB.setRingTime(this.deviceId, this.accountId, ringTime);
        this.mergedUserInfoDynamoDB.setTimeZone(this.deviceId, this.accountId, DateTimeZone.UTC);
        this.mergedUserInfoDynamoDB.setPillColor(this.deviceId, this.accountId, "Pang's Pill", new Color(0xFE, 0x00, 0x00));

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
        this.mergedUserInfoDynamoDB.setAlarms(deviceId, 0L, Collections.EMPTY_LIST);
        this.mergedUserInfoDynamoDB.setAlarms(deviceId, 1L, Collections.EMPTY_LIST);
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

}

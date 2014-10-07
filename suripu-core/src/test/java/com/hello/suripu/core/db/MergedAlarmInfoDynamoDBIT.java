package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.RingTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 9/26/14.
 */
public class MergedAlarmInfoDynamoDBIT {

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB;
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
            MergedAlarmInfoDynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.mergedAlarmInfoDynamoDB = new MergedAlarmInfoDynamoDB(
                    this.amazonDynamoDBClient,
                    tableName
            );


        }catch (ResourceInUseException rie){
            rie.printStackTrace();
        }

    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }
    }

    @Test
    public void testCreateAndRetrieve(){
        final AlarmInfo alarmInfo = new AlarmInfo(this.deviceId, this.accountId,
                Collections.EMPTY_LIST,
                Optional.<RingTime>absent(),
                Optional.of(DateTimeZone.UTC));
        this.mergedAlarmInfoDynamoDB.setInfo(alarmInfo);
        final Optional<AlarmInfo> retrieved = this.mergedAlarmInfoDynamoDB.getInfo(this.deviceId, this.accountId);
        assertThat(retrieved.isPresent(), is(true));
        assertThat(retrieved.get().timeZone.isPresent(), is(true));
    }

    @Test
    public void testUpdateNotAppend(){
        final RingTime ringTime = new RingTime(DateTime.now().getMillis(), DateTime.now().getMillis(), new long[]{1L});
        final AlarmInfo alarmInfo = new AlarmInfo(this.deviceId, this.accountId,
                Collections.EMPTY_LIST,
                Optional.of(ringTime),
                Optional.<DateTimeZone>absent());
        this.mergedAlarmInfoDynamoDB.setInfo(alarmInfo);

        final AlarmInfo updatedAlarmInfo = new AlarmInfo(this.deviceId, this.accountId,
                Collections.EMPTY_LIST,
                Optional.<RingTime>absent(),
                Optional.of(DateTimeZone.UTC));
        this.mergedAlarmInfoDynamoDB.setInfo(updatedAlarmInfo);

        final List<AlarmInfo> alarmInfoList = this.mergedAlarmInfoDynamoDB.getInfo(this.deviceId);
        assertThat(alarmInfoList.size(), is(1));
        assertThat(alarmInfoList.get(0).ringTime.isPresent(), is(true));
        assertThat(alarmInfoList.get(0).timeZone.isPresent(), is(true));
        assertThat(alarmInfoList.get(0).ringTime.get().equals(ringTime), is(true));
        assertThat(alarmInfoList.get(0).timeZone.get().equals(DateTimeZone.UTC), is(true));
    }

    @Test
    public void testGetFromNoneExistDevice(){

        final List<AlarmInfo> alarmInfoList = this.mergedAlarmInfoDynamoDB.getInfo("fitbit");
        assertThat(alarmInfoList.size(), is(0));

        assertThat(this.mergedAlarmInfoDynamoDB.getInfo("fitbit", 1L).isPresent(), is(false));
    }

}

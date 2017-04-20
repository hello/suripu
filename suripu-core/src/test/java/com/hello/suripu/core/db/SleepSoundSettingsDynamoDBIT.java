package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.sleep_sounds.SleepSoundSettingsDynamoDB;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.SleepSoundSetting;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by ksg 02/28/2017
 */

public class SleepSoundSettingsDynamoDBIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(SleepSoundSettingsDynamoDBIT.class);

    private AmazonDynamoDBClient amazonDynamoDBClient;
    private SleepSoundSettingsDynamoDB dao;
    private final String tableName = "test_table";


    @Before
    public void setUp(){
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            LOGGER.debug("-------- Creating Table {} ---------", tableName);
            SleepSoundSettingsDynamoDB.createTable(this.amazonDynamoDBClient, tableName);
            LOGGER.debug("Created dynamoDB table");
            this.dao = SleepSoundSettingsDynamoDB.create(this.amazonDynamoDBClient, tableName);
        }catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(this.tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }


    @Test
    public void testUpdateSetting() throws Exception {
        final String senseId = "sense";
        final Long accountId = 1L;
        final Optional<SleepSoundSetting> missingSettingOptional = dao.get(senseId, accountId);
        assertThat(missingSettingOptional.isPresent(), is(false));


        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Integer volumeFactor = 90;
        final Duration duration = Duration.create(1L, "30 Minutes", 1800);
        final Sound sound = Sound.create(9L,
                "https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Brown_Noise.mp",
                "Brown Noise",
                "/SLPTONES/ST001.RAW",
                "s3://hello-audio/sleep-tones-raw/2016-04-01/ST001.raw"
        );

        final SleepSoundSetting setting = SleepSoundSetting.create(senseId, accountId, now, sound, duration, volumeFactor);
        dao.update(setting);

        final Optional<SleepSoundSetting> settingOptional = dao.get(senseId, accountId);
        assertThat(settingOptional.isPresent(), is(true));
        if (settingOptional.isPresent()) {
            assertThat(settingOptional.get().sound.equals(sound), is(true));
            assertThat(settingOptional.get().datetime.equals(now), is(true));
        }

        final Duration duration2 = Duration.create(3L, "1 Hour", 3600);
        final Integer volumeFactor2 = 57;

        final SleepSoundSetting setting2 = SleepSoundSetting.create(senseId, accountId, now.plusMinutes(1), sound, duration2, volumeFactor2);
        dao.update(setting2);

        final Optional<SleepSoundSetting> settingOptional2 = dao.get(senseId, accountId);
        assertThat(settingOptional2.isPresent(), is(true));
        if (settingOptional2.isPresent()) {
            assertThat(settingOptional2.get().duration.name.equals(duration2.name), is(true));
            assertThat(settingOptional2.get().datetime.equals(now.plusMinutes(1)), is(true));
            assertThat(settingOptional2.get().volumeScalingFactor, is(volumeFactor2));
        }
    }


}
package com.hello.suripu.core.profile;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.MultiDensityImage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProfilePhotoStoreDynamoDBIT {

    private final static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ProfilePhotoStoreDynamoDBIT.class);

    private AmazonDynamoDB amazonDynamoDB;
    private ProfilePhotoStore store;
    private String tableName = "test_profile";

    @Before
    public void setUp(){
        final BasicAWSCredentials awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);

        tearDown();
        this.amazonDynamoDB = new AmazonDynamoDBClient(awsCredentials, clientConfiguration);
        this.amazonDynamoDB.setEndpoint("http://localhost:7777");

        try {
            ProfilePhotoStoreDynamoDB.createTable(this.amazonDynamoDB, tableName);
        } catch (InterruptedException ie) {
            LOGGER.warn("Table already exists");
        }
        store = ProfilePhotoStoreDynamoDB.create(amazonDynamoDB, tableName);
    }

    @After
    public void tearDown(){
        try {
            amazonDynamoDB.deleteTable(tableName);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Test
    public void testPut() {
        final MultiDensityImage image = MultiDensityImage.empty();
        final ImmutableProfilePhoto photo = ImmutableProfilePhoto
                .builder()
                .accountId(999L)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .photo(image)
                .build();

        final boolean success = store.put(photo);
        assertThat(success, is(true));
    }

    @Test
    public void testPutAndGetAndDeleteAndGet() {
        final Long accountId = 999L;
        final MultiDensityImage image = MultiDensityImage.empty();
        final ImmutableProfilePhoto photo = ImmutableProfilePhoto
                .builder()
                .accountId(accountId)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .photo(image)
                .build();

        final boolean success = store.put(photo);
        assertThat(success, is(true));
        final Optional<ImmutableProfilePhoto> photoOptional = store.get(accountId);
        assertThat(photoOptional.isPresent(), is(true));

        store.delete(accountId);
        final Optional<ImmutableProfilePhoto> optionalDeletedPhoto = store.get(accountId);
        assertThat(optionalDeletedPhoto.isPresent(), is(false));

        store.delete(accountId);
    }
}

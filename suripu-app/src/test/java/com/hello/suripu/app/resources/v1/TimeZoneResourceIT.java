package com.hello.suripu.app.resources.v1;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by pangwu on 9/19/14.
 */
public class TimeZoneResourceIT {

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;

    private TimeZoneResource timeZoneResource;
    private final String tableName = "alarm_test";


    private final AccessToken token = new AccessToken.Builder()
            .withAccountId(1L)
            .withCreatedAt(DateTime.now())
            .withExpiresIn(DateTime.now().plusHours(1).getMillis())
            .withRefreshToken(UUID.randomUUID())
            .withToken(UUID.randomUUID())
            .withScopes(new OAuthScope[]{ OAuthScope.USER_BASIC, OAuthScope.USER_BASIC })
            .withAppId(1L)
            .build();


    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            TimeZoneHistoryDAODynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(
                    this.amazonDynamoDBClient,
                    tableName
            );

            this.timeZoneResource = new TimeZoneResource(this.timeZoneHistoryDAODynamoDB);


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
    public void testUpdateTimeZone(){
        int offsetMillis  = DateTimeZone.getDefault().getOffset(DateTime.now());
        final TimeZoneHistory history = new TimeZoneHistory(offsetMillis,
                DateTime.now().getZone().getID());
        this.timeZoneResource.setTimeZone(this.token, history);

        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(this.token.accountId);
        assertThat(updated.isPresent(), is(true));

        assertThat(updated.get(), is(history));
    }

    @Test(expected = WebApplicationException.class)
    public void testUpdateTimeZoneWhenDynamoIsDown(){
        cleanUp();
        int offsetMillis  = DateTimeZone.getDefault().getOffset(DateTime.now());
        final TimeZoneHistory history = new TimeZoneHistory(offsetMillis,
                DateTime.now().getZone().getID());
        this.timeZoneResource.setTimeZone(this.token, history);

        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(this.token.accountId);
    }
}

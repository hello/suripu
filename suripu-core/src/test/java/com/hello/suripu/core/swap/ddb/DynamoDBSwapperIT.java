package com.hello.suripu.core.swap.ddb;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.swap.SwapIntent;
import com.hello.suripu.core.swap.SwapResult;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamoDBSwapperIT {

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private DynamoDBSwapper swapper;

    private final String swapTableName = "tim_swap_test";
    private final String mergedTableName = "tim_merged_test";


    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            MergedUserInfoDynamoDB.createTable(mergedTableName, this.amazonDynamoDBClient);


        }catch (ResourceInUseException rie){

        }

        try {
            DynamoDBSwapper.createTable(swapTableName, this.amazonDynamoDBClient);
        } catch (ResourceInUseException rie) {

        }
    }


    @After
    public void cleanUp(){
        final List<String> tablenames = Lists.newArrayList(mergedTableName, swapTableName);

        for(String tableName : tablenames) {
            final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                    .withTableName(tableName);

            try {
                this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
            } catch (ResourceNotFoundException ex) {

            }
        }
    }

    @Test
    public void legitSwap() {
        final DeviceDAO deviceDAO = mock(DeviceDAO.class);
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient);
        swapper = new DynamoDBSwapper(deviceDAO, dynamoDB, swapTableName, mergedTableName);

        final Table mergedTable = dynamoDB.getTable(mergedTableName);

        final Long accountId = 999L;
        final String newSenseId = "sense";
        final String currentSenseId = "current";
        final ImmutableList<DeviceAccountPair> pairs = ImmutableList.copyOf(
                Lists.newArrayList(
                        new DeviceAccountPair(accountId, 0L, currentSenseId, DateTime.now(DateTimeZone.UTC))
                )
        );

        final Item current = new Item().withPrimaryKey("device_id", currentSenseId, "account_id", accountId);
        final PutItemOutcome putItemOutcome = mergedTable.putItem(current);
        final ItemCollection<QueryOutcome> outcome = mergedTable.query(
                "device_id", currentSenseId,
                new RangeKeyCondition("account_id").gt(0)
        );
        assertEquals(1, outcome.firstPage().getLowLevelResult().getItems().size());

        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(pairs);
        when(deviceDAO.getAccountIdsForDeviceId(newSenseId)).thenReturn(ImmutableList.<DeviceAccountPair>of());
        final Optional<SwapIntent> intent = swapper.eligible(accountId, newSenseId);
        assertTrue(intent.isPresent());
        swapper.create(intent.get());

        final Optional<SwapIntent> swapIntent = swapper.query(newSenseId);
        assertTrue(swapIntent.isPresent());
        final SwapResult swapResult = swapper.swap(swapIntent.get());
        assertTrue(swapResult.successful());

        final ItemCollection<QueryOutcome> foo = mergedTable.query(
                "device_id", newSenseId,
                new RangeKeyCondition("account_id").gt(0)
        );
        assertEquals(1, foo.firstPage().getLowLevelResult().getItems().size());
    }
}

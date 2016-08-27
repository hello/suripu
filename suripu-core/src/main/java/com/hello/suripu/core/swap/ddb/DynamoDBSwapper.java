package com.hello.suripu.core.swap.ddb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.BatchWriteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.swap.Intent;
import com.hello.suripu.core.swap.IntentResult;
import com.hello.suripu.core.swap.Result;
import com.hello.suripu.core.swap.Status;
import com.hello.suripu.core.swap.Swapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DynamoDBSwapper implements Swapper {

    private final static Logger LOGGER = LoggerFactory.getLogger(DynamoDBSwapper.class);
    private final static Integer DEFAULT_QUERY_INTERVAL = 15;

    private final DeviceDAO deviceDAO;
    private final DynamoDB dynamoDB;
    private final String swapTableName;
    private final String mergedTableName;

    public DynamoDBSwapper(final DeviceDAO deviceDAO, final DynamoDB dynamoDB, final String swapTableName, final String mergedTableName) {
        this.deviceDAO = deviceDAO;
        this.dynamoDB = dynamoDB;
        this.swapTableName = swapTableName;
        this.mergedTableName = mergedTableName;
    }

    @Override
    public Result swap(final Intent intent) {

        final Table table = dynamoDB.getTable(mergedTableName);

        final ItemCollection<QueryOutcome> queryOutcomeItemCollection = table.query(
                new KeyAttribute("device_id", intent.currentSenseId())
        );

        final List<Item> itemList = queryOutcomeItemCollection.firstPage().getLowLevelResult().getItems();

        if(itemList.isEmpty()) {
            return Result.failed(Result.Error.SOMETHING_ELSE);
        }

        final List<Long> accountIds = new ArrayList<>();
        final List<Item> updatedItems = new ArrayList<>();
        for(Item item : itemList) {
            final Item updated = item.withPrimaryKey(
                    "device_id", intent.newSenseId(),
                    "account_id", item.getLong("account_id")
            );
            accountIds.add(item.getLong("account_id"));
            updatedItems.add(updated);
        }

        final TableWriteItems items = new TableWriteItems(mergedTableName).withItemsToPut(updatedItems);
        final BatchWriteItemOutcome outcome = dynamoDB.batchWriteItem(items);

        if(!outcome.getUnprocessedItems().isEmpty()) {
            return Result.failed(Result.Error.SOMETHING_ELSE);
        }

        int accountSwapped = 0;
        for(final Long accountId : accountIds) {
            try {
                deviceDAO.registerSense(accountId, intent.newSenseId());
                accountSwapped += 1;
            } catch (Exception e) {
                LOGGER.error("action=swap-sense error=failed-registration sense_id={} account_id={}", intent.newSenseId(), accountId);
            }
        }

        return (accountSwapped == accountIds.size()) ? Result.success() : Result.failed(Result.Error.SOMETHING_ELSE);
    }

    @Override
    public void create(Intent intent) {
        final Table table = dynamoDB.getTable(swapTableName);

        final Item item = new Item()
                .withPrimaryKey(
                        "sense_id", intent.newSenseId(),
                        "attempted_at", intent.dateTime().toString())
                .withString("current_sense_id", intent.currentSenseId())
                .withLong("account_id", intent.accountId());

        table.putItem(item);
    }

    @Override
    public Optional<Intent> query(final String senseId) {
        return query(senseId, DEFAULT_QUERY_INTERVAL);
    }

    @Override
    public Optional<Intent> query(String senseId, int minutesAgo) {
        final Table table = dynamoDB.getTable(swapTableName);
        final String start = DateTime.now(DateTimeZone.UTC).minusMinutes(minutesAgo).toString();
        final String end = DateTime.now(DateTimeZone.UTC).plusMinutes(1).toString();

        final ItemCollection<QueryOutcome> queryOutcomeItemCollection = table.query(new KeyAttribute("sense_id", senseId),
                new RangeKeyCondition("attempted_at").between(start, end)
        );

        final List<Item> items = queryOutcomeItemCollection.firstPage().getLowLevelResult().getItems();
        if(items.isEmpty()) {
            LOGGER.warn("action=query-swap result=no-swap-intent sense_id={} start={} end={}", senseId, start, end);
            return Optional.absent();
        }
        // Assuming ordered list
        final Item item =  items.get(items.size()-1);
        final Intent intent = Intent.create(
                item.getString("current_sense_id"),
                item.getString("sense_id"),
                item.getLong("account_id"),
                DateTime.parse(item.getString("attempted_at"))
        );
        return Optional.of(intent);
    }

    @Override
    public IntentResult eligible(final Long accountId, String newSenseId) {
        final List<DeviceAccountPair> pairedAccountsCurrentSense = deviceDAO.getSensesForAccountId(accountId);
        if(pairedAccountsCurrentSense.size() != 1) {
            return IntentResult.failed(Status.ACCOUNT_PAIRED_TO_MULTIPLE_SENSE);
        }

        final List<DeviceAccountPair> pairedAccountsNewSense = deviceDAO.getAccountIdsForDeviceId(newSenseId);
        final boolean pairedToSameAccount = pairedAccountsNewSense.size() == 1 && pairedAccountsNewSense.get(0).accountId == accountId;

        if(pairedAccountsNewSense.isEmpty() || pairedToSameAccount) {
            final Intent intent = Intent.create(
                    pairedAccountsCurrentSense.get(0).externalDeviceId,
                    newSenseId,
                    accountId);
            return IntentResult.ok(intent);
        }

        return IntentResult.failed(Status.NEW_SENSE_PAIRED_TO_DIFFERENT_ACCOUNT);
    }

    public static CreateTableResult createTable(final String swapTableName, final AmazonDynamoDB client) {
        final List<AttributeDefinition> attributes = ImmutableList.of(
                new AttributeDefinition().withAttributeName("sense_id").withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName("attempted_at").withAttributeType(ScalarAttributeType.S)
        );

        // Keys
        final List<KeySchemaElement> keySchema = ImmutableList.of(
                new KeySchemaElement().withAttributeName("sense_id").withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName("attempted_at").withKeyType(KeyType.RANGE)
        );

        final ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L);

        final CreateTableRequest request = new CreateTableRequest()
                .withTableName(swapTableName)
                .withAttributeDefinitions(attributes)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(provisionedThroughput);

        return client.createTable(request);
    }
}

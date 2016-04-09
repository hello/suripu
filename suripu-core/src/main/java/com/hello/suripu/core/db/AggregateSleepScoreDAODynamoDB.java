package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AggregateScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AggregateSleepScoreDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(AggregateSleepScoreDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;
    private final String version;
    public final Set<String> targetAttributes;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";
    public static final String DATE_ATTRIBUTE_NAME = "date";
    public static final String SCORE_ATTRIBUTE_NAME = "score";
    public static final String TYPE_ATTRIBUTE_NAME = "type";
    public static final String VERSION_ATTRIBUTE_NAME = "version";
    private static final int MAX_CALL_COUNT = 5;



    public static final String DEFAULT_SCORE_TYPE = "sleep";

    public AggregateSleepScoreDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName, final String version) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName + "_" + version;
        this.version = version;
        this.targetAttributes = new HashSet<>();
        Collections.addAll(targetAttributes, ACCOUNT_ID_ATTRIBUTE_NAME, DATE_ATTRIBUTE_NAME, SCORE_ATTRIBUTE_NAME, TYPE_ATTRIBUTE_NAME, VERSION_ATTRIBUTE_NAME);
    }

    public void writeSingleScore(final AggregateScore score) {
        LOGGER.debug("Write single score: {}, {}, {}", score.accountId, score.date, score.score);
        final HashMap<String, AttributeValue> item = this.createItem(score);
        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, item);
        final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);

    }

    public Boolean updateInsertSingleScore(final Long accountId, final Integer score, final String date) {
        LOGGER.debug("Write single score: {}, {}, {}", accountId, date, score);


        try {
            final HashMap<String, AttributeValueUpdate> item = this.createUpdateItem(score);

            final Map<String, AttributeValue> key = new HashMap<>();
            key.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
            key.put(DATE_ATTRIBUTE_NAME, new AttributeValue().withS(date));

            final Map<String, ExpectedAttributeValue> putConditions = new HashMap<>();
            putConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, new ExpectedAttributeValue(
                    new AttributeValue().withN(String.valueOf(accountId))));
            putConditions.put(DATE_ATTRIBUTE_NAME, new ExpectedAttributeValue(
                    new AttributeValue().withS(date)));


            final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(this.tableName)
                    .withKey(key)
                    .withAttributeUpdates(item)
                    .withExpected(putConditions)
                    .withReturnValues(ReturnValue.ALL_NEW);

            final UpdateItemResult result = this.dynamoDBClient.updateItem(updateItemRequest);
            if (result.getAttributes().size() > 0) {
                return true;
            }
        } catch (AmazonServiceException ase) {
            LOGGER.error("Failed to update sleep score for account {}, date {}, score {}",
                    accountId, date, score);
        }
        return false;

    }

    public void writeBatchScores(final List<AggregateScore> scores) {

        final List<WriteRequest> scoreList = new ArrayList<>();

        for (final AggregateScore score : scores) {
                LOGGER.debug("Batch: {}, {}, {}", score.accountId, score.date, score.score);
                final HashMap<String, AttributeValue> item = this.createItem(score);
                scoreList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(item)));
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put(this.tableName, scoreList);

        BatchWriteItemResult result;
        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();

        do {
            batchWriteItemRequest.withRequestItems(requestItems);
            result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);

            // check for unprocessed items
            requestItems = result.getUnprocessedItems();
            LOGGER.debug("Unprocessed put request count {}", requestItems.size());
        } while (result.getUnprocessedItems().size() > 0);

    }

    public AggregateScore getSingleScore(final Long accountId, final String date) {

        final Map<String, AttributeValue> key = new HashMap<>();
        key.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
        key.put(DATE_ATTRIBUTE_NAME, new AttributeValue().withS(date));

        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(this.tableName)
                .withKey(key)
                .withAttributesToGet(this.targetAttributes);

        final GetItemResult result = this.dynamoDBClient.getItem(getItemRequest);
        final Map<String, AttributeValue> item = result.getItem();

        if (item == null) {
            LOGGER.debug("Account {} date {} score not found", accountId, date);
            return new AggregateScore(accountId, 0, date, DEFAULT_SCORE_TYPE, this.version);
        }

        if(!item.keySet().containsAll(this.targetAttributes)){
            LOGGER.warn("Missing field in item {}", item);
            return new AggregateScore(accountId, 0, date, DEFAULT_SCORE_TYPE, this.version);
        }

        return this.createAggregateScore(item);

    }

    public ImmutableList<AggregateScore> getBatchScores(final Long accountId, final String startDate, final String endDate, final int numDays){

        final Condition selectByAccountId  = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));

        final Condition selectByDate = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withS(startDate),
                        new AttributeValue().withS(endDate));

        final Map<String, Condition> queryConditions = new HashMap<>();
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectByAccountId);
        queryConditions.put(DATE_ATTRIBUTE_NAME, selectByDate);

        final List<AggregateScore> scoreResults = new ArrayList<>();

        Map<String, AttributeValue> lastEvaluatedKey = null;
        int loopCount = 0;

        do {
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(this.targetAttributes)
                    .withLimit(numDays)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            if (queryResult.getItems() != null) {
                for (final Map<String, AttributeValue> item : items) {
                    if (!item.keySet().containsAll(targetAttributes)) {
                        LOGGER.warn("Missing field in item {}", item);
                        continue;
                    }
                    final AggregateScore score = this.createAggregateScore(item);
                    scoreResults.add(score);
                }
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;

        } while (lastEvaluatedKey != null && loopCount < MAX_CALL_COUNT);


        Collections.sort(scoreResults);
        return ImmutableList.copyOf(scoreResults);

    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){

        // attributes
        ArrayList<AttributeDefinition> attributes = new ArrayList<AttributeDefinition>();
        attributes.add(new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType("N"));
        attributes.add(new AttributeDefinition().withAttributeName(DATE_ATTRIBUTE_NAME).withAttributeType("S"));

        // keys
        ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
        keySchema.add(new KeySchemaElement().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH));
        keySchema.add(new KeySchemaElement().withAttributeName(DATE_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE));

        // throughput provision
        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L);

        final CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributes)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(provisionedThroughput);

        return dynamoDBClient.createTable(request);

    }

    private HashMap<String, AttributeValue> createItem(AggregateScore score) {
        final HashMap<String, AttributeValue> item = new HashMap<>();
        item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(score.accountId)));
        item.put(DATE_ATTRIBUTE_NAME, new AttributeValue().withS(score.date));
        item.put(SCORE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(score.score)));
        item.put(TYPE_ATTRIBUTE_NAME, new AttributeValue().withS(score.scoreType));
        item.put(VERSION_ATTRIBUTE_NAME, new AttributeValue().withS(score.version));

        return item;
    }

    private HashMap<String, AttributeValueUpdate> createUpdateItem(final Integer score) {
        final HashMap<String, AttributeValueUpdate> item = new HashMap<>();

        item.put(SCORE_ATTRIBUTE_NAME, new AttributeValueUpdate().withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(score))));

        item.put(TYPE_ATTRIBUTE_NAME, new AttributeValueUpdate().withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(this.DEFAULT_SCORE_TYPE)));

        item.put(VERSION_ATTRIBUTE_NAME, new AttributeValueUpdate().withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(this.version)));

        return item;
    }

    private AggregateScore createAggregateScore(Map<String, AttributeValue> item) {
        return new AggregateScore(
                Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN()),
                Integer.valueOf(item.get(SCORE_ATTRIBUTE_NAME).getN()),
                item.get(DATE_ATTRIBUTE_NAME).getS(),
                item.get(TYPE_ATTRIBUTE_NAME).getS(),
                item.get(VERSION_ATTRIBUTE_NAME).getS()
        );
    }

}

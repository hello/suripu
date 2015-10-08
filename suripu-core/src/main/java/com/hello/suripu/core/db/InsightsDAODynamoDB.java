package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.Select;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InsightsDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(InsightsDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;
    public final Set<String> targetAttributes;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id"; // primary key
    public static final String DATE_CATEGORY_ATTRIBUTE_NAME = "date_category"; // range key
    public static final String CATEGORY_ATTRIBUTE_NAME = "category";
    public static final String TIME_PERIOD_ATTRIBUTE_NAME = "time_period";
    public static final String TITLE_ATTRIBUTE_NAME = "title";
    public static final String MESSAGE_ATTRIBUTE_NAME = "message";
    public static final String TIMESTAMP_UTC_ATTRIBUTE_NAME = "timestamp_utc";
    private static final int MAX_CALL_COUNT = 5;



    public static final String DEFAULT_SCORE_TYPE = "sleep";

    public InsightsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;

        this.targetAttributes = new HashSet<>();
        Collections.addAll(targetAttributes, ACCOUNT_ID_ATTRIBUTE_NAME, DATE_CATEGORY_ATTRIBUTE_NAME,
                CATEGORY_ATTRIBUTE_NAME, TIME_PERIOD_ATTRIBUTE_NAME,
                TITLE_ATTRIBUTE_NAME, MESSAGE_ATTRIBUTE_NAME, TIMESTAMP_UTC_ATTRIBUTE_NAME);
    }

    @Timed
    public void insertInsight(final InsightCard insightCard) {
        LOGGER.debug("write single insight {}, {}, {}", insightCard.accountId, insightCard.category, insightCard.timestamp);
        final HashMap<String, AttributeValue> item = this.createItem(insightCard);
        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, item);
        final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);
        LOGGER.debug("write single insight {}", result.toString());

    }

    @Timed
    public void insertListOfInsights(final List<InsightCard> insightCards) {
        final List<WriteRequest> insights = new ArrayList<>();
        for (InsightCard insightCard : insightCards) {
            final HashMap<String, AttributeValue> item = this.createItem(insightCard);
            final PutRequest putRequest = new PutRequest().withItem(item);
            insights.add(new WriteRequest().withPutRequest(putRequest));
        }

        // batch-write
        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put(this.tableName, insights);

        BatchWriteItemResult results;
        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();

        do {
            batchWriteItemRequest.withRequestItems(requestItems);
            results = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);
        } while (results.getUnprocessedItems().size() > 0);

        if (results.getUnprocessedItems().size() > 0) {
            LOGGER.error("Batch write insights fail to write {} records", results.getUnprocessedItems().size());
            LOGGER.error("First insight in batch is account {}, category", insightCards.get(0).accountId, insightCards.get(0).category);
        }
    }

    @Timed
    public ImmutableList<InsightCard> getInsightsByDate(final Long accountId, final DateTime date, final Boolean chronological, final int limit) {

        final Condition selectByAccountId = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));

        final Condition selectByDate;
        if (chronological) { // ascending date
            final String rangeKey = this.createDateCategoryKey(date, "000");
            selectByDate = new Condition()
                    .withComparisonOperator(ComparisonOperator.GE.toString())
                    .withAttributeValueList(new AttributeValue().withS(rangeKey));
        } else { // reverse chronological
            final String rangeKey = this.createDateCategoryKey(date, "ZZZ");
            selectByDate = new Condition()
                    .withComparisonOperator(ComparisonOperator.LE.toString())
                    .withAttributeValueList(new AttributeValue().withS(rangeKey));
        }

        final Map<String, Condition> queryConditions = new HashMap<>();
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectByAccountId);
        queryConditions.put(DATE_CATEGORY_ATTRIBUTE_NAME, selectByDate);

        return this.getData(queryConditions, limit, chronological);
    }

    @Timed
    public int getInsightCountByDate(final Long accountId, final DateTime date, final int limit) {
        final Condition selectByAccountId = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));

        final String rangeKey = this.createDateCategoryKey(date, "000");
        final Condition selectByDate = new Condition()
                .withComparisonOperator(ComparisonOperator.GE.toString())
                .withAttributeValueList(new AttributeValue().withS(rangeKey));

        final Map<String, Condition> queryConditions = new HashMap<>();
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectByAccountId);
        queryConditions.put(DATE_CATEGORY_ATTRIBUTE_NAME, selectByDate);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withSelect(Select.COUNT)
                .withLimit(limit);

        final QueryResult queryResult = dynamoDBClient.query(queryRequest);
        return queryResult.getCount();
    }

    @Timed
    public ImmutableList<InsightCard> getInsightsByCategory(final Long accountId, final InsightCard.Category category, final int limit) {

        final Condition selectByAccountId = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));

        final DateTime beginningOfTime = DateTime.parse("2014-01-01", DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC);
        final String rangeKey = this.createDateCategoryKey(beginningOfTime, category.toCategoryString());

        final Condition selectByCategory = new Condition()
                .withComparisonOperator(ComparisonOperator.GE.toString())
                .withAttributeValueList(new AttributeValue().withS(rangeKey));

        final Map<String, Condition> queryConditions = new HashMap<>();
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectByAccountId);
        queryConditions.put(DATE_CATEGORY_ATTRIBUTE_NAME, selectByCategory);

        return this.getData(queryConditions, limit, true);
    }

    private ImmutableList<InsightCard> getData(final Map<String, Condition> queryConditions, final int limit, final Boolean scanForward) {
        final List<InsightCard> insightCards = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;
        int loopCount = 0;

        do {
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(this.targetAttributes)
                    .withLimit(limit)
                    .withScanIndexForward(scanForward)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            if (queryResult.getItems() != null) {
                for (final Map<String, AttributeValue> item : items) {
                    if (!item.keySet().containsAll(targetAttributes)) {
                        LOGGER.warn("Missing field in item {}", item);
                        continue;
                    }
                    final InsightCard card = this.createInsightCard(item);
                    insightCards.add(card);
                }
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;

        } while (lastEvaluatedKey != null && loopCount < MAX_CALL_COUNT && insightCards.size() < limit);

        Collections.sort(insightCards, Collections.reverseOrder());

        return ImmutableList.copyOf(insightCards);

    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){

        // attributes
        ArrayList<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType("N"));
        attributes.add(new AttributeDefinition().withAttributeName(DATE_CATEGORY_ATTRIBUTE_NAME).withAttributeType("S"));

        // keys
        ArrayList<KeySchemaElement> keySchema = new ArrayList<>();
        keySchema.add(new KeySchemaElement().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH));
        keySchema.add(new KeySchemaElement().withAttributeName(DATE_CATEGORY_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE));

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

    private String createDateCategoryKey(final DateTime date, final String category) {
        return DateTimeUtil.dateToYmdString(date) + "_" + category;
    }

    private HashMap<String, AttributeValue> createItem(final InsightCard insightCard) {
        final HashMap<String, AttributeValue> item = new HashMap<>();
        item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(insightCard.accountId.get())));

        final DateTime dateTime = new DateTime(insightCard.timestamp, DateTimeZone.UTC);

        final String ymdCategory = this.createDateCategoryKey(dateTime.withTimeAtStartOfDay(), insightCard.category.toCategoryString());

        item.put(DATE_CATEGORY_ATTRIBUTE_NAME, new AttributeValue().withS(ymdCategory));

        item.put(CATEGORY_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(insightCard.category.getValue())));
        item.put(TIME_PERIOD_ATTRIBUTE_NAME, new AttributeValue().withS(insightCard.timePeriod.toString()));
        item.put(TITLE_ATTRIBUTE_NAME, new AttributeValue().withS(insightCard.title));
        item.put(MESSAGE_ATTRIBUTE_NAME, new AttributeValue().withS(insightCard.message));
        item.put(TIMESTAMP_UTC_ATTRIBUTE_NAME, new AttributeValue().withS(insightCard.timestamp.toString()));
        return item;
    }

    private InsightCard createInsightCard(Map<String, AttributeValue> item) {
        final InsightCard.Category category = InsightCard.Category.fromInteger(Integer.valueOf(item.get(CATEGORY_ATTRIBUTE_NAME).getN()));
        final InsightCard.TimePeriod timePeriod = InsightCard.TimePeriod.fromString(item.get(TIME_PERIOD_ATTRIBUTE_NAME).getS());

        return new InsightCard(
                Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN()),
                item.get(TITLE_ATTRIBUTE_NAME).getS(),
                item.get(MESSAGE_ATTRIBUTE_NAME).getS(),
                category,
                timePeriod,
                new DateTime(item.get(TIMESTAMP_UTC_ATTRIBUTE_NAME).getS(), DateTimeZone.UTC)
        );
    }

}

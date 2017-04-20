package com.hello.suripu.core.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Expressions;
import com.hello.suripu.core.db.dynamo.expressions.Expression;
import com.hello.suripu.core.db.responses.Response;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 11/18/15.
 *
 * Abstract class for a time series stored in DynamoDB, sharded by time.
 */
public abstract class TimeSeriesDAODynamoDB<T> {

    protected static final int MAX_PUT_ITEMS = 25;

    protected final AmazonDynamoDB dynamoDBClient;
    protected final String tablePrefix;

    public TimeSeriesDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tablePrefix) {
        this.dynamoDBClient = dynamoDBClient;
        this.tablePrefix = tablePrefix;
    }


    //region abstract methods
    protected abstract Logger logger();
    protected abstract Integer maxQueryAttempts();
    protected abstract Integer maxBatchWriteAttempts();

    protected abstract String hashKeyName();
    protected abstract String rangeKeyName();
    protected abstract String hashKeyType();
    protected abstract String rangeKeyType();
    protected abstract String getHashKey(final AttributeValue attributeValue);
    protected abstract String getRangeKey(final AttributeValue attributeValue);

    protected abstract DateTime getTimestamp(final T model);
    protected abstract Map<String, AttributeValue> toAttributeMap(final T model);

    public abstract String getTableName(final DateTime dateTime);

    /**
     * Return table names from start to end, in chronological order.
     * @param start
     * @param end
     * @return
     */
    public abstract List<String> getTableNames(final DateTime start, final DateTime end);
    //endregion



    //region Inserts
    public CreateTableResult createTable(final String tableName) {
        // attributes
        ArrayList<AttributeDefinition> attributes = Lists.newArrayList();
        attributes.add(new AttributeDefinition().withAttributeName(hashKeyName()).withAttributeType(hashKeyType()));
        attributes.add(new AttributeDefinition().withAttributeName(rangeKeyName()).withAttributeType(rangeKeyType()));

        // keys
        ArrayList<KeySchemaElement> keySchema = Lists.newArrayList();
        keySchema.add(new KeySchemaElement().withAttributeName(hashKeyName()).withKeyType(KeyType.HASH));
        keySchema.add(new KeySchemaElement().withAttributeName(rangeKeyName()).withKeyType(KeyType.RANGE));

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

    /**
     * Convert a list of T to write request items, which can be used for batchInsert.
     * @param modelList
     * @return Map of {tableName => writeRequests}
     */
    public Map<String, List<WriteRequest>> toWriteRequestItems(final List<T> modelList) {
        // Create a map with hash+range as the key to deduplicate and avoid DynamoDB exceptions
        final Map<String, Map<String, WriteRequest>> writeRequestMap = Maps.newHashMap();
        for (final T data: modelList) {
            final String tableName = getTableName(getTimestamp(data));
            final Map<String, AttributeValue> item = toAttributeMap(data);
            final String hashAndRangeKey = getHashKey(item.get(hashKeyName())) + getRangeKey(item.get(rangeKeyName()));
            final WriteRequest request = new WriteRequest().withPutRequest(new PutRequest().withItem(item));
            if (writeRequestMap.containsKey(tableName)) {
                writeRequestMap.get(tableName).put(hashAndRangeKey, request);
            } else {
                final Map<String, WriteRequest> newMap = Maps.newHashMap();
                newMap.put(hashAndRangeKey, request);
                writeRequestMap.put(tableName, newMap);
            }
        }

        final Map<String, List<WriteRequest>> requestItems = Maps.newHashMapWithExpectedSize(writeRequestMap.size());
        for (final Map.Entry<String, Map<String, WriteRequest>> entry : writeRequestMap.entrySet()) {
            requestItems.put(entry.getKey(), Lists.newArrayList(entry.getValue().values()));
        }

        return requestItems;
    }

    /**
     * Batch insert write requests.
     * Subject to DynamoDB's maximum BatchWriteItem size.
     * Utilizes exponential backoff in case of throttling.
     *
     * @param requestItems - map of {tableName => writeRequests}
     * @return the remaining unprocessed items. The return value of this method can be used to call this method again
     * to process remaining items.
     */
    public Map<String, List<WriteRequest>> batchInsert(final Map<String, List<WriteRequest>> requestItems) {
        int numAttempts = 0;
        Map<String, List<WriteRequest>> remainingItems = requestItems;

        do {
            if (numAttempts > 0) {
                // Being throttled! Back off, buddy.
                backoff(numAttempts);
            }

            numAttempts++;
            final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(remainingItems);
            final BatchWriteItemResult result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);
            // check for unprocessed items
            remainingItems = result.getUnprocessedItems();
        } while (!remainingItems.isEmpty() && (numAttempts < maxBatchWriteAttempts()));

        return remainingItems;
    }

    /**
     * Batch insert list of T objects.
     * Subject to DynamoDB's maximum BatchWriteItem size.
     * @param modelList
     * @return The number of successfully inserted elements.
     */
    public int batchInsert(final List<T> modelList) {

        final Map<String, List<WriteRequest>> requestItems = toWriteRequestItems(modelList);
        final Map<String, List<WriteRequest>> remainingItems = batchInsert(requestItems);

        final int totalItemsToInsert = countWriteRequestItems(requestItems);

        if (!remainingItems.isEmpty()) {
            final int remainingItemsCount = countWriteRequestItems(remainingItems);
            logger().warn("Exceeded {} attempts to batch write to Dynamo. {} items left over.",
                    maxBatchWriteAttempts(), remainingItemsCount);
            return totalItemsToInsert - remainingItemsCount;
        }

        return totalItemsToInsert;
    }

    /**
     * batch insert a list of T objects, no retry. (used by mainly by migration)
     * @param modelList
     * @return return items that failed
     */
    public List<Map<String, AttributeValue>> batchInsertNoRetryReturnsRemaining(final List<T> modelList) {
        final Map<String, List<WriteRequest>> requestItems = toWriteRequestItems(modelList);
        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(requestItems);

        final BatchWriteItemResult result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);

        final Map<String, List<WriteRequest>> remainingItems = result.getUnprocessedItems();

        final List<Map<String, AttributeValue>> remainingData = Lists.newArrayList();
        for (final List<WriteRequest> requests : remainingItems.values()) {
            for (final WriteRequest request: requests) {
                final Map<String, AttributeValue> item = request.getPutRequest().getItem();
                if (item != null && !item.isEmpty()) {
                    remainingData.add(item);
                }
            }
        }

        return remainingData;
    }


    /**
     * Partitions and inserts list of objects.
     * @param modelList
     * @return The number of items that were successfully inserted
     */
    public int batchInsertAllPartitions(final List<T> modelList) {
        final List<List<T>> modelLists = Lists.partition(modelList, MAX_PUT_ITEMS);
        int successfulInsertions = 0;

        // Insert each chunk
        for (final List<T> modelListToWrite: modelLists) {
            try {
                successfulInsertions += batchInsert(modelListToWrite);
            } catch (AmazonClientException e) {
                logger().error("Got exception while attempting to batchInsert to DynamoDB: {}", e);
            }

        }

        return successfulInsertions;
    }

    private int countWriteRequestItems(final Map<String, List<WriteRequest>> requestItems) {
        int total = 0;
        for (final List<WriteRequest> writeRequests : requestItems.values()) {
            total += writeRequests.size();
        }
        return total;
    }
    //endregion


    /**
     * Convert String:AttributeValue map to key=val formatted log string.
     * Only works for String or Number AttributeValues.
     */
    @VisibleForTesting
    static String attributeValueMapToLogString(final Map<String, AttributeValue> attributeValueMap) {
        final List<String> entries = Lists.newArrayList();
        for (final Map.Entry<String, AttributeValue> entry: attributeValueMap.entrySet()) {
            if (entry.getValue().getN() != null) {
                entries.add(entry.getKey() + "=" + entry.getValue().getN());
            } else if (entry.getValue().getS() != null) {
                entries.add(entry.getKey() + "=" + entry.getValue().getS());
            }
        }
        return Joiner.on(" ").join(entries);
    }

    //region Query
    protected Response<List<Map<String, AttributeValue>>> query(final QueryRequest originalQueryRequest) {
        final List<Map<String, AttributeValue>> results = Lists.newArrayList();

        Map<String, AttributeValue> lastEvaluatedKey = null;
        int numAttempts = 0;
        boolean keepTrying = true;

        do {
            numAttempts++;
            final QueryRequest queryRequest = originalQueryRequest
                    .clone()
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult;
            try {
                queryResult = this.dynamoDBClient.query(queryRequest);
            } catch (ProvisionedThroughputExceededException ptee) {
                if (numAttempts >= maxQueryAttempts()) {
                    logger().warn("exception=ProvisionedThroughputExceededException method=query table={} {}",
                            queryRequest.getTableName(),
                            attributeValueMapToLogString(queryRequest.getExpressionAttributeValues()));
                    return Response.partial(results, ptee);
                }
                backoff(numAttempts);
                continue;
            } catch (ResourceNotFoundException rnfe) {
                // Invalid table name
                logger().error("Got ResourceNotFoundException while attempting to read from table {}; {}",
                        queryRequest.getTableName(),
                        rnfe);
                return Response.failure(results, rnfe);
            }
            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            if (queryResult.getItems() != null) {
                for (final Map<String, AttributeValue> item : items) {
                    results.add(item);
                }
            }

            if ((originalQueryRequest.getLimit() != null) &&
                    (results.size() >= originalQueryRequest.getLimit()))
            {
                return Response.success(results);
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            keepTrying = (lastEvaluatedKey != null);

        } while (keepTrying && (numAttempts < maxQueryAttempts()));

        if (lastEvaluatedKey != null) {
            logger().warn("Exceeded {} attempts while querying. Stopping with last evaluated key: {}",
                    maxQueryAttempts(), lastEvaluatedKey);
            return Response.partial(results);
        }

        return Response.success(results);

    }

    private Response<List<Map<String, AttributeValue>>> query(final String tableName,
                                                              final String keyConditionExpression,
                                                              final Collection<? extends Attribute> targetAttributes,
                                                              final Optional<String> filterExpression,
                                                              final Map<String, AttributeValue> filterAttributeValues)
    {
        final Map<String, String> expressionAttributeNames = Expressions.expressionAttributeNames(targetAttributes);
        final String projectionExpression = Expressions.projectionExpression(targetAttributes);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withProjectionExpression(projectionExpression)
                .withExpressionAttributeNames(expressionAttributeNames)
                .withKeyConditionExpression(keyConditionExpression)
                .withExpressionAttributeValues(filterAttributeValues);

        if (filterExpression.isPresent()) {
            queryRequest.setFilterExpression(filterExpression.get());
        }

        return query(queryRequest);
    }

    protected Optional<Map<String, AttributeValue>> getLatest(final String tableName,
                                                              final Expression keyConditionExpression,
                                                              final Collection<? extends Attribute> attributes)
    {
        final List<Map<String, AttributeValue>> latestRecords = getNumLatest(tableName, keyConditionExpression, attributes, 1);
        if(latestRecords.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(latestRecords.get(0));
    }

    protected List<Map<String, AttributeValue>> getNumLatest(final String tableName,
                                                                 final Expression keyConditionExpression,
                                                                 final Collection<? extends Attribute> attributes,
                                                                 final Integer limit)
    {
        final Map<String, String> expressionAttributeNames = Expressions.expressionAttributeNames(attributes);
        final String projectionExpression = Expressions.projectionExpression(attributes);

        final QueryRequest queryRequest = new QueryRequest()
            .withTableName(tableName)
            .withProjectionExpression(projectionExpression)
            .withExpressionAttributeNames(expressionAttributeNames)
            .withKeyConditionExpression(keyConditionExpression.expressionString())
            .withExpressionAttributeValues(keyConditionExpression.expressionAttributeValues())
            .withScanIndexForward(false)
            .withLimit(limit);

        final Response<List<Map<String, AttributeValue>>> response = query(queryRequest);
        if (response.status == Response.Status.FAILURE || response.data.isEmpty()) {
            return Lists.newArrayList();
        }

        return Lists.reverse(response.data);
    }

    protected Response<List<Map<String, AttributeValue>>> queryTables(final Iterable<String> tableNames,
                                                                      final Expression keyConditionExpression,
                                                                      final Collection<? extends Attribute> attributes)
    {
        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        final String keyCondition = keyConditionExpression.expressionString();
        for (final String table: tableNames) {
            final Response<List<Map<String, AttributeValue>>> response = query(table, keyCondition, attributes, Optional.<String>absent(), keyConditionExpression.expressionAttributeValues());
            if (response.status == Response.Status.SUCCESS) {
                results.addAll(response.data);
            } else {
                return Response.into(results, response);
            }
        }
        return Response.success(results);
    }

    private ImmutableMap<String,AttributeValue> getAllAttributeValues(final Expression keyConditionExpression,
                                                                      final Expression filterExpression)
    {
        return new ImmutableMap.Builder<String, AttributeValue>()
                .putAll(keyConditionExpression.expressionAttributeValues())
                .putAll(filterExpression.expressionAttributeValues())
                .build();
    }

    protected Response<List<Map<String, AttributeValue>>> queryTables(final Iterable<String> tableNames,
                                                                      final Expression keyConditionExpression,
                                                                      final Expression filterExpression,
                                                                      final Collection<? extends Attribute> attributes)
    {
        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        final String keyCondition = keyConditionExpression.expressionString();
        final String filterCondition = filterExpression.expressionString();
        final Map<String,AttributeValue> attributeValues = getAllAttributeValues(keyConditionExpression, filterExpression);
        for (final String table: tableNames) {
            final Response<List<Map<String, AttributeValue>>> response = query(
                    table, keyCondition, attributes, Optional.of(filterCondition), attributeValues);
            if (response.status == Response.Status.SUCCESS) {
                results.addAll(response.data);
            } else {
                return Response.into(results, response);
            }
        }
        return Response.success(results);
    }
    //endregion


    protected void backoff(int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            logger().warn("Throttled by DynamoDB, sleeping for {} ms.", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            logger().error("Interrupted while attempting exponential backoff.");
        }
    }
}

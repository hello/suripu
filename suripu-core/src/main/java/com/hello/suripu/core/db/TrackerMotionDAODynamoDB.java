package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 5/30/14.
 */
@Deprecated
public class TrackerMotionDAODynamoDB {
    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerMotionDAODynamoDB.class);
    private final AmazonDynamoDBClient dynamoDBClient;
    private final String tableName;

    public final static String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";
    public final static String TARGET_DATE_ATTRIBUTE_NAME = "target_date_utc_timestamp";
    public final static String DATA_BLOB_ATTRIBUTE_NAME = "data_blob";

    public final static int MAX_REQUEST_DAYS = 31;
    public final static int MAX_UPLOAD_DATA_COUNT = 4 * 24 * 60;  // Does not allow upload more than 4 days data.
    public final static int MAX_CALL_COUNT = 5;


    public TrackerMotionDAODynamoDB(final AmazonDynamoDBClient amazonDynamoDBClient,
                                    final String tableName){
        this.dynamoDBClient = amazonDynamoDBClient;
        this.tableName = tableName;

    }

    /*
    * Get tracker data for maybe not consecutive days, internal use only
     */
    private ImmutableMap<String, ImmutableList<TrackerMotion>> getTrackerMotionForDateStrings(long accountId, final Collection<String> dateStrings){
        if(dateStrings.size() > MAX_REQUEST_DAYS){
            LOGGER.warn("Request too large for events, num of days requested: {}, accountId: {}, eventType: {}", dateStrings.size(), accountId);
            throw new RuntimeException("Request too many days event.");
        }

        final Map<String, ImmutableList<TrackerMotion>> finalResult = new HashMap<String, ImmutableList<TrackerMotion>>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final String[] sortedDateStrings = dateStrings.toArray(new String[0]);
        Arrays.sort(sortedDateStrings);

        final String startDateString = sortedDateStrings[0];
        final String endDateString = sortedDateStrings[sortedDateStrings.length - 1];

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withS(startDateString),
                        new AttributeValue().withS(endDateString));


        queryConditions.put(TARGET_DATE_ATTRIBUTE_NAME, selectDateCondition);

        // AND accound_id = :accound_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        Map<String, AttributeValue> lastEvaluatedKey = null;
        final Collection<String> targetAttributeSet = new HashSet<String>();
        Collections.addAll(targetAttributeSet, TARGET_DATE_ATTRIBUTE_NAME, DATA_BLOB_ATTRIBUTE_NAME);

        int loopCount = 0;

        do{
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(targetAttributeSet)
                    .withLimit(MAX_REQUEST_DAYS)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            if(queryResult.getItems() != null){
                final List<Map<String, AttributeValue>> items = queryResult.getItems();
                for(final Map<String, AttributeValue> item:items){
                    if(!item.keySet().containsAll(targetAttributeSet)){
                        LOGGER.warn("Missing field in item {}", item);
                        continue;
                    }

                    final ByteBuffer byteBuffer = item.get(DATA_BLOB_ATTRIBUTE_NAME).getB();

                    //System.out.println("Bin protobuf size: " + byteBuffer.array().length);
                    final String dateString = item.get(TARGET_DATE_ATTRIBUTE_NAME).getS();

                    if(!dateStrings.contains(dateString)){
                        continue;
                    }

                    try {
                        final InputProtos.TrackerDataBatch trackerDataBatch = InputProtos.TrackerDataBatch.parseFrom(byteBuffer.array());
                        final List<InputProtos.TrackerDataBatch.TrackerData> dataList = trackerDataBatch.getSamplesList();
                        final List<TrackerMotion> resultForDate = new LinkedList<TrackerMotion>();

                        for(final InputProtos.TrackerDataBatch.TrackerData datum:dataList){
                            TrackerMotion trackerMotion = new TrackerMotion(
                                    -1,
                                    accountId,
                                    "",
                                    datum.getTimestamp(),
                                    datum.getSvmNoGravity(),
                                    datum.getOffsetMillis());
                            resultForDate.add(trackerMotion);
                        }

                        finalResult.put(dateString, ImmutableList.copyOf(resultForDate));

                        //System.out.println("Actual model count: " + resultForDate.size());
                    } catch (InvalidProtocolBufferException e) {
                        LOGGER.error("Corrupted data for account_id: {}, date: {}, {}", accountId, dateString, e.getMessage());
                    }
                }
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;
        }while (lastEvaluatedKey != null && loopCount < MAX_CALL_COUNT);

        if(dateStrings.size() > MAX_REQUEST_DAYS){
            LOGGER.warn("Request too many for data, num of days requested: {}, accountId: {}", dateStrings.size(), accountId);
            throw new RuntimeException("Request too much data.");
        }


        // Fill the non-exist days with empty lists
        for(final String dateString:dateStrings){
            if(!finalResult.containsKey(dateString)){
                finalResult.put(dateString, ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));
            }
        }

        return ImmutableMap.copyOf(finalResult);
    }

    public ImmutableMap<DateTime, ImmutableList<TrackerMotion>> getTrackerMotionForDates(long accountId, final Collection<DateTime> dates){
        final Map<String, DateTime> dateToStringMapping = new HashMap<String, DateTime>();
        for(final DateTime date:dates){
            dateToStringMapping.put(date.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT), date);
        }

        final Collection<String> dateStrings = dateToStringMapping.keySet();
        final ImmutableMap<String, ImmutableList<TrackerMotion>> data = this.getTrackerMotionForDateStrings(accountId, dateStrings);

        final Map<DateTime, ImmutableList<TrackerMotion>> finalResultMap = new HashMap<DateTime, ImmutableList<TrackerMotion>>();
        for(final String dateString:data.keySet()){
            if(dateToStringMapping.containsKey(dateString)){
                finalResultMap.put(dateToStringMapping.get(dateString), data.get(dateString));
            }
        }

        return ImmutableMap.copyOf(finalResultMap);
    }


    private ImmutableList<TrackerMotion> getTrackerMotionForDate(long accountId, final String targetDateString) {
        final HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        key.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
        key.put(TARGET_DATE_ATTRIBUTE_NAME, new AttributeValue().withS(targetDateString));

        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(this.tableName)
                .withKey(key);

        GetItemResult getItemResult = null;
        try {
            getItemResult = dynamoDBClient.getItem(getItemRequest);
        }catch (AmazonServiceException ase){
            LOGGER.error("Amazon service exception {}", ase.getMessage());
        }finally {
            if (getItemResult.getItem() == null || !getItemResult.getItem().containsKey(DATA_BLOB_ATTRIBUTE_NAME)) {
                return ImmutableList.copyOf(Collections.<TrackerMotion>emptyList());
            }
        }

        final ByteBuffer byteBuffer = getItemResult.getItem().get(DATA_BLOB_ATTRIBUTE_NAME).getB();

        try {
            final InputProtos.TrackerDataBatch trackerDataBatch = InputProtos.TrackerDataBatch.parseFrom(byteBuffer.array());
            final List<InputProtos.TrackerDataBatch.TrackerData> dataList = trackerDataBatch.getSamplesList();

            LinkedList<TrackerMotion> resultData = new LinkedList<TrackerMotion>();

            for(final InputProtos.TrackerDataBatch.TrackerData datum:dataList){
                TrackerMotion trackerMotion = new TrackerMotion(
                        -1,
                        accountId,
                        "",
                        datum.getTimestamp(),
                        datum.getSvmNoGravity(),
                        datum.getOffsetMillis());
                resultData.add(trackerMotion);

            }

            return ImmutableList.copyOf(resultData);

        } catch (InvalidProtocolBufferException e) {

            LOGGER.error("Error in deserializing data for account {}, date {}, error: {}",
                    accountId,
                    targetDateString,
                    e.getMessage());
        }

        return ImmutableList.copyOf(Collections.<TrackerMotion>emptyList());
    }

    public ImmutableList<TrackerMotion> getTrackerMotionForDate(long accountId, final DateTime targetDateLocal) {
        return getTrackerMotionForDate(accountId, targetDateLocal.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT));
    }


    public ImmutableList<TrackerMotion> getBetween(long accountId,
                                                   final DateTime startTimestampLocal,
                                                   final DateTime endTimestampLocal) {
        if(startTimestampLocal.getMillis() > endTimestampLocal.getMillis()){
            return ImmutableList.copyOf(Collections.<TrackerMotion>emptyList());
        }

        if(endTimestampLocal.getMillis() > startTimestampLocal.plusDays(MAX_REQUEST_DAYS).getMillis()){
            return ImmutableList.copyOf(Collections.<TrackerMotion>emptyList());
        }


        final DateTime queryStartDateLocal = startTimestampLocal.withTimeAtStartOfDay();
        final DateTime queryEndDateLocal = endTimestampLocal.withTimeAtStartOfDay();

        // WHERE target_date >= :start_date AND target_date <= :end_date
        final Condition selectDateRangeCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withS(queryStartDateLocal.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)),
                        new AttributeValue().withS(queryEndDateLocal.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)));

        // AND accound_id = :accound_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));

        final HashMap<String, Condition> queryConditions = new HashMap<String, Condition>();
        queryConditions.put(TARGET_DATE_ATTRIBUTE_NAME, selectDateRangeCondition);
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        Map<String, AttributeValue> lastEvaluatedKey = null;
        final LinkedList<TrackerMotion> finalResult = new LinkedList<TrackerMotion>();

        int loopCount = 0;

        do{
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(DATA_BLOB_ATTRIBUTE_NAME)
                    .withLimit(MAX_REQUEST_DAYS)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            if(queryResult.getItems() != null){
                final List<Map<String, AttributeValue>> items = queryResult.getItems();
                for(final Map<String, AttributeValue> item:items){
                    if(!item.containsKey(DATA_BLOB_ATTRIBUTE_NAME)){
                        LOGGER.warn("Missing field: {}", DATA_BLOB_ATTRIBUTE_NAME);
                        continue;
                    }

                    final ByteBuffer byteBuffer = item.get(DATA_BLOB_ATTRIBUTE_NAME).getB();

                    try {
                        final InputProtos.TrackerDataBatch trackerDataBatch = InputProtos.TrackerDataBatch.parseFrom(byteBuffer.array());
                        final List<InputProtos.TrackerDataBatch.TrackerData> dataList = trackerDataBatch.getSamplesList();

                        for(final InputProtos.TrackerDataBatch.TrackerData datum:dataList){
                            if(datum.getTimestamp() >= startTimestampLocal.getMillis() &&
                                    datum.getTimestamp() <= endTimestampLocal.getMillis()) {
                                TrackerMotion trackerMotion = new TrackerMotion(
                                        -1,
                                        accountId,
                                        "",
                                        datum.getTimestamp(),
                                        datum.getSvmNoGravity(),
                                        datum.getOffsetMillis());
                                finalResult.add(trackerMotion);
                            }
                        }
                    } catch (InvalidProtocolBufferException e) {
                        LOGGER.error("Error in deserializing data for account {}, date range {}-{}, error: {}",
                                accountId,
                                queryStartDateLocal.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT),
                                queryEndDateLocal.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT),
                                e.getMessage());
                    }
                }
            }
            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;
        }while (lastEvaluatedKey != null && loopCount <= MAX_CALL_COUNT);

        return ImmutableList.copyOf(finalResult);
    }


    public void setTrackerMotions(long accountId, final List<TrackerMotion> data) {
        if(data.size() == 0){
            LOGGER.info("Empty motion data for account_id = {}", accountId);
            return;
        }

        if(data.size() > MAX_UPLOAD_DATA_COUNT){
            LOGGER.error("Account: {} tries to upload large data, data size: {}", accountId, data.size());
            throw new RuntimeException("data is too large");
        }

        final HashMap<String, InputProtos.TrackerDataBatch.Builder> groupedData =
                new HashMap<String, InputProtos.TrackerDataBatch.Builder>();

        // Group the data based on dates
        for(final TrackerMotion datum:data){
            final DateTime localTime = new DateTime(datum.timestamp, DateTimeZone.forOffsetMillis(datum.offsetMillis));

            String dateKey = localTime.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT);

            /*
            * DO NOT cut off by noon because the firmware cut off data at midnight.
            *
            final DateTime localStartOfDay = localTime.withTimeAtStartOfDay();

            if(localTime.getMillis() < localStartOfDay.plusHours(12).getMillis()){
                // If the data is collected before noon, this is the data of previous day.
                dateKey = localTime.minusDays(1).toString(DateTimeFormatString.DYNAMO_DB_DATE_FORMAT);
            }
            */


            if(!groupedData.containsKey(dateKey)){
                groupedData.put(dateKey, InputProtos.TrackerDataBatch.newBuilder());
            }

            final InputProtos.TrackerDataBatch.TrackerData trackerData = InputProtos.TrackerDataBatch.TrackerData.newBuilder()
                    .setOffsetMillis(datum.offsetMillis)
                    .setTimestamp(datum.timestamp)
                    .setSvmNoGravity(datum.value)
                    .build();
            groupedData.get(dateKey).addSamples(trackerData);
        }


        final ArrayList<WriteRequest> putRequests = new ArrayList<WriteRequest>();

        for(final String dateKey:groupedData.keySet()){
            final HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
            item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
            item.put(TARGET_DATE_ATTRIBUTE_NAME, new AttributeValue().withS(dateKey));
            final ByteBuffer byteBuffer = ByteBuffer.wrap(groupedData.get(dateKey).build().toByteArray());
            item.put(DATA_BLOB_ATTRIBUTE_NAME, new AttributeValue().withB(byteBuffer));
            final PutRequest putItemRequest = new PutRequest()
                    .withItem(item);
            final WriteRequest writeRequest = new WriteRequest().withPutRequest(putItemRequest);
            putRequests.add(writeRequest);

        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        requestItems.put(this.tableName, putRequests);

        BatchWriteItemResult result;
        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest()
                .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
        int callCount = 0;


        do {

            batchWriteItemRequest.withRequestItems(requestItems);
            result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);
            requestItems = result.getUnprocessedItems();
            callCount++;
        } while (result.getUnprocessedItems().size() > 0 && callCount <= MAX_CALL_COUNT);

        if(result.getUnprocessedItems().size() > 0){
            LOGGER.error("Account: {} tries to upload large data, data size: {}", accountId, data.size());
            throw new RuntimeException("data is too large");
        }



    }

}

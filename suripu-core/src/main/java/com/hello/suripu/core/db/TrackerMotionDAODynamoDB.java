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
import com.hello.suripu.core.db.util.DateTimeFormatString;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 5/30/14.
 */
public class TrackerMotionDAODynamoDB {
    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerMotionDAODynamoDB.class);
    private final AmazonDynamoDBClient dynamoDBClient;
    private final String tableName;

    public final static String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";
    public final static String TARGET_DATE_ATTRIBUTE_NAME = "target_date_utc_timestamp";
    private final static String DATA_BLOB_ATTRIBUTE_NAME = "json_data_blob";


    public TrackerMotionDAODynamoDB(final AmazonDynamoDBClient amazonDynamoDBClient,
                                    final String tableName){
        this.dynamoDBClient = amazonDynamoDBClient;
        this.tableName = tableName;

    }

    private ImmutableMap<String, List<TrackerMotion>> getTrackerMotionForDateStrings(long accountId, final Collection<String> dateStrings){
        if(dateStrings.size() > 31){
            return ImmutableMap.copyOf(Collections.<String, List<TrackerMotion>>emptyMap());
        }

        final Map<String, List<TrackerMotion>> finalResult = new HashMap<String, List<TrackerMotion>>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        String[] sortedDateStrings = dateStrings.toArray(new String[0]);
        Arrays.sort(sortedDateStrings);

        final String startDateString = sortedDateStrings[0];
        final String endDateString = sortedDateStrings[sortedDateStrings.length - 1];

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withS(startDateString),
                        new AttributeValue().withS(endDateString));

        long maxLoopCount = (DateTime.parse(endDateString, DateTimeFormat.forPattern(DateTimeFormatString.FORMAT_TO_DAY)).getMillis()
                - DateTime.parse(startDateString, DateTimeFormat.forPattern(DateTimeFormatString.FORMAT_TO_DAY)).getMillis()) /
                (24 * 60 * 60 * 1000) + 1;


        queryConditions.put(TARGET_DATE_ATTRIBUTE_NAME, selectDateCondition);

        // AND accound_id = :accound_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        Map<String, AttributeValue> lastEvaluatedKey = null;


        int loopCount = 0;

        do{
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(TARGET_DATE_ATTRIBUTE_NAME, DATA_BLOB_ATTRIBUTE_NAME)
                    .withLimit(10)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            if(queryResult.getItems() != null){
                final List<Map<String, AttributeValue>> items = queryResult.getItems();
                for(final Map<String, AttributeValue> item:items){
                    if(item.containsKey(DATA_BLOB_ATTRIBUTE_NAME) == false ||
                            item.containsKey(TARGET_DATE_ATTRIBUTE_NAME) == false){
                        LOGGER.warn("Missing field in item {}", item);
                        continue;
                    }

                    final ByteBuffer byteBuffer = item.get(DATA_BLOB_ATTRIBUTE_NAME).getB();
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

                        finalResult.put(dateString, resultForDate);
                    } catch (InvalidProtocolBufferException e) {

                        LOGGER.error("{}", e.getStackTrace());
                    }
                }
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;
        }while (lastEvaluatedKey != null && loopCount < maxLoopCount);

        // Fill the non-exist days with empty lists
        for(final String dateString:dateStrings){
            if(!finalResult.containsKey(dateString)){
                finalResult.put(dateString, Collections.<TrackerMotion>emptyList());
            }
        }

        return ImmutableMap.copyOf(finalResult);
    }

    public ImmutableMap<DateTime, List<TrackerMotion>> getTrackerMotionForDates(long accountId, final Collection<DateTime> dates){
        final Map<String, DateTime> dateToStringMapping = new HashMap<String, DateTime>();
        for(final DateTime date:dates){
            dateToStringMapping.put(date.toString(DateTimeFormatString.FORMAT_TO_DAY), date);
        }

        final Collection<String> dateStrings = dateToStringMapping.keySet();
        final ImmutableMap<String, List<TrackerMotion>> data = this.getTrackerMotionForDateStrings(accountId, dateStrings);

        final Map<DateTime, List<TrackerMotion>> finalResultMap = new HashMap<DateTime, List<TrackerMotion>>();
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

        } catch (IOException e) {

            LOGGER.error("{}", e.getStackTrace());
        }

        return ImmutableList.copyOf(Collections.<TrackerMotion>emptyList());
    }

    public ImmutableList<TrackerMotion> getTrackerMotionForDate(long accountId, final DateTime targetDateLocal) {
        return getTrackerMotionForDate(accountId, targetDateLocal.toString(DateTimeFormatString.FORMAT_TO_DAY));
    }


    public ImmutableList<TrackerMotion> getBetween(long accountId,
                                                   final DateTime startTimestampLocal,
                                                   final DateTime endTimestampLocal) {
        if(startTimestampLocal.getMillis() > endTimestampLocal.getMillis()){
            return ImmutableList.copyOf(Collections.<TrackerMotion>emptyList());
        }

        if(endTimestampLocal.getMillis() > startTimestampLocal.plusDays(31).getMillis()){
            return ImmutableList.copyOf(Collections.<TrackerMotion>emptyList());
        }


        final DateTime queryStartDateLocal = startTimestampLocal.withTimeAtStartOfDay();
        final DateTime queryEndDateLocal = endTimestampLocal.withTimeAtStartOfDay();

        // WHERE target_date >= :start_date AND target_date <= :end_date
        final Condition selectDateRangeCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withS(queryStartDateLocal.toString(DateTimeFormatString.FORMAT_TO_DAY)),
                        new AttributeValue().withS(queryEndDateLocal.toString(DateTimeFormatString.FORMAT_TO_DAY)));

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
                    .withLimit(10)
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
                            TrackerMotion trackerMotion = new TrackerMotion(
                                    -1,
                                    accountId,
                                    "",
                                    datum.getTimestamp(),
                                    datum.getSvmNoGravity(),
                                    datum.getOffsetMillis());
                            finalResult.add(trackerMotion);
                        }
                    } catch (InvalidProtocolBufferException e) {

                        LOGGER.error("{}", e.getStackTrace());
                    }
                }
            }
            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;
        }while (lastEvaluatedKey != null && loopCount < 30);

        return ImmutableList.copyOf(finalResult);
    }


    public boolean isAccountInitialized(long accountId) {

        final DateTime queryStartDateLocal = new DateTime(1970,1,1,0,0,0);

        // WHERE target_date >= :start_date AND target_date <= :end_date
        final Condition selectDateRangeCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.GE.toString())
                .withAttributeValueList(new AttributeValue().withS(queryStartDateLocal.toString(DateTimeFormatString.FORMAT_TO_DAY)));

        // AND accound_id = :accound_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));

        final HashMap<String, Condition> queryConditions = new HashMap<String, Condition>();
        queryConditions.put(TARGET_DATE_ATTRIBUTE_NAME, selectDateRangeCondition);
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        Map<String, AttributeValue> lastEvaluatedKey = null;
        final LinkedList<TrackerMotion> finalResult = new LinkedList<TrackerMotion>();

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(DATA_BLOB_ATTRIBUTE_NAME)
                .withLimit(1)
                .withExclusiveStartKey(lastEvaluatedKey);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        if(queryResult.getItems() != null){
            final List<Map<String, AttributeValue>> items = queryResult.getItems();
            if(items.size() == 0){
                return false;
            }

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
                        TrackerMotion trackerMotion = new TrackerMotion(
                                -1,
                                accountId,
                                "",
                                datum.getTimestamp(),
                                datum.getSvmNoGravity(),
                                datum.getOffsetMillis());
                        finalResult.add(trackerMotion);
                    }
                } catch (InvalidProtocolBufferException e) {

                    LOGGER.error("{}", e.getStackTrace());
                }
            }
        }else{
            return false;
        }

        return true;
    }

    public void setTrackerMotions(long accountId, final List<TrackerMotion> data) {
        if(data.size() == 0){
            return;
        }


        final HashMap<String, InputProtos.TrackerDataBatch.Builder> groupedData =
                new HashMap<String, InputProtos.TrackerDataBatch.Builder>();

        // Group the data based on dates
        for(final TrackerMotion datum:data){
            final DateTime localTime = new DateTime(datum.timestamp, DateTimeZone.forOffsetMillis(datum.offsetMillis));

            final DateTime localStartOfDay = localTime.withTimeAtStartOfDay();

            String dateKey = localTime.toString(DateTimeFormatString.FORMAT_TO_DAY);

            if(localTime.getMillis() < localStartOfDay.plusHours(12).getMillis()){
                // If the data is collected before noon, this is the data of previous day.
                dateKey = localTime.minusDays(1).toString(DateTimeFormatString.FORMAT_TO_DAY);
            }

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
        } while (result.getUnprocessedItems().size() > 0 && callCount < 100);



    }

}

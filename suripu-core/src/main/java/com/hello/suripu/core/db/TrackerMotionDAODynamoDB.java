package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.util.DateTimeFormatString;
import com.hello.suripu.core.models.AmplitudeDataCompact;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
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


    public ImmutableList<TrackerMotion> getTrackerMotionForDate(long accountId, final String targetDateString) {
        final HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        key.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
        key.put(TARGET_DATE_ATTRIBUTE_NAME, new AttributeValue().withS(targetDateString));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(this.tableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);

        if(getItemResult.getItem() == null || !getItemResult.getItem().containsKey(DATA_BLOB_ATTRIBUTE_NAME)) {
            return ImmutableList.copyOf(new TrackerMotion[0]);
        }

        final String jasonCompactMotionData = getItemResult.getItem().get(DATA_BLOB_ATTRIBUTE_NAME).getS();
        ObjectMapper mapper = new ObjectMapper();
        try {
            final List<AmplitudeDataCompact> data = mapper.readValue(jasonCompactMotionData,
                    new TypeReference<List<AmplitudeDataCompact>>(){});

            TrackerMotion[] resultData = new TrackerMotion[data.size()];

            int index = 0;
            for(final AmplitudeDataCompact datum:data){
                TrackerMotion trackerMotion = new TrackerMotion(
                        -1,
                        accountId,
                        "",
                        datum.timestamp,
                        datum.amplitude,
                        datum.offsetMillis);
                resultData[index] = trackerMotion;
                index++;
            }

            return ImmutableList.copyOf(resultData);

        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.warn("{}", e.getStackTrace());
        }

        return ImmutableList.copyOf(new TrackerMotion[0]);
    }

    public ImmutableList<TrackerMotion> getTrackerMotionForDate(long accountId, final DateTime targetDateLocal) {
        return getTrackerMotionForDate(accountId, targetDateLocal.toString(DateTimeFormatString.FORMAT_TO_DAY));
    }


    public ImmutableList<TrackerMotion> getBetween(long accountId,
                                                   final DateTime startTimestampLocal,
                                                   final DateTime endTimestampLocal) {
        if(startTimestampLocal.getMillis() > endTimestampLocal.getMillis()){
            return ImmutableList.copyOf(new TrackerMotion[0]);
        }

        long delta = endTimestampLocal.getMillis() - startTimestampLocal.getMillis();
        if(endTimestampLocal.getMillis() > startTimestampLocal.plusDays(31).getMillis()){
            return ImmutableList.copyOf(new TrackerMotion[0]);
        }


        final ArrayList<String> queryDates = new ArrayList<String>();
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
                        continue;
                    }

                    final String jasonCompactMotionData = item.get(DATA_BLOB_ATTRIBUTE_NAME).getS();
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        final List<AmplitudeDataCompact> data = mapper.readValue(jasonCompactMotionData,
                                new TypeReference<List<AmplitudeDataCompact>>(){});

                        for(final AmplitudeDataCompact datum:data){
                            TrackerMotion trackerMotion = new TrackerMotion(
                                    -1,
                                    accountId,
                                    "",
                                    datum.timestamp,
                                    datum.amplitude,
                                    datum.offsetMillis);
                            finalResult.add(trackerMotion);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        LOGGER.warn("{}", e.getStackTrace());
                    }
                }
            }
            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
        }while (lastEvaluatedKey != null);

        return ImmutableList.copyOf(finalResult.toArray(new TrackerMotion[0]));








        /*




        DateTime currentQueryDateLocal = queryStartDateLocal;
        // Get all possible dates in the query time range
        while (currentQueryDateLocal.getMillis() <= queryEndDateLocal.getMillis()){
            queryDates.add(currentQueryDateLocal.toString(DateTimeFormatString.FORMAT_TO_DAY));
            currentQueryDateLocal = currentQueryDateLocal.plusDays(1);
        }

        final LinkedList<TrackerMotion> result = new LinkedList<TrackerMotion>();
        long startTimestamp = startTimestampLocal.getMillis();
        long endTimestamp = endTimestampLocal.getMillis();

        // Filter data between time range
        for(final String dateString:queryDates){
            ImmutableList<TrackerMotion> motionsForADay = getTrackerMotionForDate(accountId, dateString);
            for(final TrackerMotion motion:motionsForADay){
                if(motion.timestamp >= startTimestamp && motion.timestamp <= endTimestamp){
                    result.add(motion);
                }
            }
        }
        return ImmutableList.copyOf(result.toArray(new TrackerMotion[0]));
        */

    }

    public void setTrackerMotions(long accountId, final List<TrackerMotion> data) {
        final HashMap<String, LinkedList<AmplitudeDataCompact>> groupedData =
                new HashMap<String, LinkedList<AmplitudeDataCompact>>();

        // Group the data based on dates
        for(final TrackerMotion datum:data){
            final DateTime localTime = new DateTime(datum.timestamp, DateTimeZone.forOffsetMillis(datum.offsetMillis));
            final String dateKey = localTime.toString(DateTimeFormatString.FORMAT_TO_DAY);
            if(!groupedData.containsKey(dateKey)){
                groupedData.put(dateKey, new LinkedList<AmplitudeDataCompact>());
            }

            groupedData.get(dateKey).add(new AmplitudeDataCompact(datum.timestamp, datum.value, datum.offsetMillis));
        }

        for(final String dateKey:groupedData.keySet()){
            final HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
            item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
            item.put(TARGET_DATE_ATTRIBUTE_NAME, new AttributeValue().withS(dateKey));

            final ObjectMapper mapper = new ObjectMapper();

            try {
                final String jsonData = mapper.writeValueAsString(groupedData.get(dateKey));
                item.put(DATA_BLOB_ATTRIBUTE_NAME, new AttributeValue().withS(jsonData));
                final PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName(this.tableName)
                        .withItem(item);

                final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);

                LOGGER.debug("Save data: {}", putItemResult);
            }catch (Exception ex){
                LOGGER.warn("Save motion data to dynamoDB failed: {}", ex.getMessage());
            }
        }

    }

}

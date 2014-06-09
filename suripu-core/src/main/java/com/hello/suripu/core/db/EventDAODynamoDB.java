package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.util.Compress;
import com.hello.suripu.core.db.util.DateTimeFormatString;
import com.hello.suripu.core.models.Event;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 6/5/14.
 */
public class EventDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerMotionDAODynamoDB.class);
    private final AmazonDynamoDBClient dynamoDBClient;
    private final String tableName;

    private final Event.Type eventType;


    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    public static final String TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME = "target_date_of_night";
    public static final String DATA_BLOB_ATTRIBUTE_NAME = "events_data";

    public static final String COMPRESS_TYPE_ARRTIBUTE_NAME = "algorithm";


    private final int MAX_CALL_COUNT = 5;
    public final int MAX_REQUEST_DAYS = 31;

    public final String JSON_CHARSET = "UTF-8";


    public EventDAODynamoDB(final AmazonDynamoDBClient dynamoDBClient, final String tableName, final Event.Type eventType){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
        this.eventType = eventType;
    }

    public ImmutableList<Event> getEventsForDate(long accountId, final DateTime date){
        final Collection<DateTime> convertedParam = new ArrayList<DateTime>();
        convertedParam.add(date);
        final ImmutableMap<DateTime, ImmutableList<Event>> result = this.getEventsForDates(accountId, convertedParam);
        return result.get(date);
    }

    public ImmutableMap<DateTime, ImmutableList<Event>> getEventsForDates(long accountId, final Collection<DateTime> dates){
        final Map<String, DateTime> dateToStringMapping = new HashMap<String, DateTime>();
        for(final DateTime date:dates){
            dateToStringMapping.put(date.toString(DateTimeFormatString.FORMAT_TO_DAY), date);
        }

        final Collection<String> dateStrings = dateToStringMapping.keySet();
        final ImmutableMap<String, ImmutableList<Event>> data = this.getEventsForDateStrings(accountId, dateStrings);

        final Map<DateTime, ImmutableList<Event>> finalResultMap = new HashMap<DateTime, ImmutableList<Event>>();
        for(final String dateString:data.keySet()){
            if(dateToStringMapping.containsKey(dateString)){
                finalResultMap.put(dateToStringMapping.get(dateString), data.get(dateString));
            }
        }

        return ImmutableMap.copyOf(finalResultMap);
    }

    /*
    * Get events for maybe not consecutive days, internal use only
     */
    private ImmutableMap<String, ImmutableList<Event>> getEventsForDateStrings(long accountId, final Collection<String> dateStrings){
        if(dateStrings.size() > MAX_REQUEST_DAYS){
            LOGGER.warn("Request too large for events, num of days requested: {}, accountId: {}, table: {}", dateStrings.size(), accountId, this.tableName);
            throw new RuntimeException("Request too many days event.");
        }

        final Map<String, ImmutableList<Event>> finalResult = new HashMap<String, ImmutableList<Event>>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final String[] sortedDateStrings = dateStrings.toArray(new String[0]);
        Arrays.sort(sortedDateStrings);

        final String startDateString = sortedDateStrings[0];
        final String endDateString = sortedDateStrings[sortedDateStrings.length - 1];

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withS(startDateString),
                        new AttributeValue().withS(endDateString));


        queryConditions.put(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME, selectDateCondition);

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
                    .withAttributesToGet(
                            TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME,
                            DATA_BLOB_ATTRIBUTE_NAME,
                            COMPRESS_TYPE_ARRTIBUTE_NAME)
                    .withLimit(MAX_REQUEST_DAYS)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            if(queryResult.getItems() != null){
                final List<Map<String, AttributeValue>> items = queryResult.getItems();
                for(final Map<String, AttributeValue> item:items){
                    if(item.containsKey(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME) == false ||
                            item.containsKey(DATA_BLOB_ATTRIBUTE_NAME) == false ||
                            item.containsKey(COMPRESS_TYPE_ARRTIBUTE_NAME) == false){
                        LOGGER.warn("Missing field in item {}", item);
                        continue;
                    }

                    final String dateString = item.get(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME).getS();
                    final ArrayList<Event> eventsWithAllTypes = new ArrayList<Event>();

                    final ByteBuffer byteBuffer = item.get(DATA_BLOB_ATTRIBUTE_NAME).getB();
                    final byte[] compressed = byteBuffer.array();

                    final Compress.CompressionType algorithmName = Compress.CompressionType.fromInt(Integer.valueOf(item.get(COMPRESS_TYPE_ARRTIBUTE_NAME).getN()));


                    try {
                        byte[] decompressed = null;
                        switch (algorithmName){
                            case GZIP:
                                decompressed = Compress.gzipDecompress(compressed);
                                break;
                            case BZIP2:
                                decompressed = Compress.bzip2Decompress(compressed);
                                break;
                        }

                        final ObjectMapper mapper = new ObjectMapper();
                        final List<Event> eventList = mapper.readValue(decompressed, new TypeReference<List<Event>>() {});
                        eventsWithAllTypes.addAll(eventList);

                    }catch (JsonParseException jpe){
                        LOGGER.error("Parsing event list for account {}, failed: {}",
                                accountId,
                                //dataAttributeName,
                                jpe.getMessage());

                    }catch (JsonMappingException jmp){
                        LOGGER.error("Parsing event list for account {}, failed: {}",
                                accountId,
                                //dataAttributeName,
                                jmp.getMessage());

                    }catch (IOException ioe){

                        LOGGER.error("Decompress event list for account {}, failed: {}",
                                accountId,
                                //dataAttributeName,
                                ioe.getMessage());
                    }


                    final Event[] rawEventsArrayWithAllTypes = eventsWithAllTypes.toArray(new Event[0]);
                    Arrays.sort(rawEventsArrayWithAllTypes, new Comparator<Event>() {
                        @Override
                        public int compare(Event o1, Event o2) {
                            return Long.compare(o1.startTimestamp, o2.startTimestamp);
                        }
                    });

                    finalResult.put(dateString, ImmutableList.copyOf(rawEventsArrayWithAllTypes));

                }
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;
        }while (lastEvaluatedKey != null && loopCount < MAX_CALL_COUNT);

        if(lastEvaluatedKey != null){
            // We still have something not fetched. Request still too large!
            LOGGER.warn("Request too large for events, num of days requested: {}, accountId: {}, tableName: {}", dateStrings.size(), accountId, this.tableName);
            throw new RuntimeException("Request too many days event.");

        }

        // Fill the non-exist days with empty lists
        for(final String dateString:dateStrings){
            if(!finalResult.containsKey(dateString)){
                finalResult.put(dateString, ImmutableList.copyOf(Collections.<Event>emptyList()));
            }
        }

        return ImmutableMap.copyOf(finalResult);
    }


    public void setEventsForDate(long accountId, final DateTime dateOfTheNight, final List<Event> data){
        final Map<DateTime, List<Event>> convertedParam = new HashMap<DateTime, List<Event>>();
        convertedParam.put(dateOfTheNight, data);
        setEventsForDates(accountId, convertedParam);
    }

    public void setEventsForDates(long accountId, final Map<DateTime, List<Event>> data){
        final Map<String, List<Event>> dataWithStringDates = new HashMap<String, List<Event>>();

        for(final DateTime dateOfTheNight:data.keySet()){
            dataWithStringDates.put(dateOfTheNight.toString(DateTimeFormatString.FORMAT_TO_DAY), data.get(dateOfTheNight));
        }

        setEventsForStringDates(accountId, dataWithStringDates);
    }

    private void setEventsForStringDates(long accountId, final Map<String, List<Event>> data){
        if(data.size() == 0){
            LOGGER.info("Empty motion data for account_id = {}", accountId);
            return;
        }

        if(data.size() > MAX_REQUEST_DAYS){
            LOGGER.error("Account: {} tries to upload large data, day count: {}", accountId, data.size());
            throw new RuntimeException("data is too large");
        }

        final ArrayList<WriteRequest> putRequests = new ArrayList<WriteRequest>();
        long itemCount = 0;

        for(final String targetDateOfNight:data.keySet()) {

            final List<Event> events = data.get(targetDateOfNight);
            final ObjectMapper mapper = new ObjectMapper();

            try {
                final String jsonEventList = mapper.writeValueAsString(events);

                final HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
                item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
                item.put(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME, new AttributeValue().withS(targetDateOfNight));
                item.put(COMPRESS_TYPE_ARRTIBUTE_NAME, new AttributeValue().withN(
                        String.valueOf(Compress.CompressionType.BZIP2.getValue())));

                // final ByteBuffer byteBuffer = ByteBuffer.wrap(builder.build().toByteArray());

                final byte[] compressedData = Compress.bzip2Compress(jsonEventList.getBytes(JSON_CHARSET));
                final ByteBuffer byteBuffer = ByteBuffer.wrap(compressedData);
                item.put(DATA_BLOB_ATTRIBUTE_NAME, new AttributeValue().withB(byteBuffer));


                final PutRequest putItemRequest = new PutRequest()
                        .withItem(item);

                final WriteRequest writeRequest = new WriteRequest().withPutRequest(putItemRequest);
                putRequests.add(writeRequest);

            }catch (JsonProcessingException jpe){
                LOGGER.error("Serialize events for account {}, night {} failed: {}",
                        accountId,
                        //type,
                        targetDateOfNight,
                        jpe.getMessage());
            }catch (UnsupportedEncodingException uee) {
                LOGGER.error("Serialize events for account {}, night {} failed: {}",
                        accountId,
                        //type,
                        targetDateOfNight,
                        uee.getMessage());
            }catch (IOException ioe){
                LOGGER.error("Compress events for account {}, night {} failed: {}",
                        accountId,
                        //type,
                        targetDateOfNight,
                        ioe.getMessage());
            }


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
            LOGGER.error("Account: {} tries to upload large event data, data size: {}", accountId, itemCount);
            throw new RuntimeException("data is too large");
        }
    }
}

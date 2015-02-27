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
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.util.Compression;
import com.hello.suripu.core.models.CachedTimelines;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.yammer.dropwizard.json.GuavaExtrasModule;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 6/5/14.
 */
public class TimelineDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    public static final String TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME = "target_date_of_night";
    public static final String DATA_BLOB_ATTRIBUTE_NAME = "timelines";

    public static final String COMPRESS_TYPE_ATTRIBUTE_NAME = "compression_type";
    public static final String UPDATED_AT_ATTRIBUTE_NAME = "updated_at";

    public static final String VERSION = "version";


    private final int MAX_CALL_COUNT = 5;
    private final int MAX_BATCH_SIZE = 25;  // Based on: http://docs.aws.amazon.com/cli/latest/reference/dynamodb/batch-write-item.html

    public static final int MAX_REQUEST_DAYS = 31;

    public final String JSON_CHARSET = "UTF-8";

    private static ObjectMapper mapper = new ObjectMapper();

    public TimelineDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;

        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new GuavaExtrasModule());
        mapper.registerModule(new JodaModule());
    }

    @Timed
    public ImmutableList<Timeline> getTimelinesForDate(long accountId, final DateTime targetDateOfNightLocalUTC){
        final Collection<DateTime> convertedParam = new ArrayList<DateTime>();
        convertedParam.add(targetDateOfNightLocalUTC);
        final ImmutableMap<DateTime, ImmutableList<Timeline>> result = this.getTimelinesForDates(accountId, convertedParam);
        if(result.containsKey(targetDateOfNightLocalUTC)){
            return result.get(targetDateOfNightLocalUTC);
        }

        return ImmutableList.copyOf(Collections.EMPTY_LIST);
    }

    @Timed
    public ImmutableMap<DateTime, ImmutableList<Timeline>> getTimelinesForDates(long accountId, final Collection<DateTime> dates){
        final Map<Long, DateTime> dateToStringMapping = new HashMap<>();
        for(final DateTime date:dates){
            dateToStringMapping.put(date.getMillis(), date);
        }

        final Collection<Long> datesInMillis = dateToStringMapping.keySet();
        final ImmutableMap<Long, CachedTimelines> cachedData = this.getTimelinesForDatesImpl(accountId, datesInMillis);

        final Map<DateTime, ImmutableList<Timeline>> finalResultMap = new HashMap<>();
        final DateTime now = DateTime.now();
        for(final Long dateInMillis:cachedData.keySet()){
            if(dateToStringMapping.containsKey(dateInMillis)){
                if(shouldInvalidateCache(cachedData.get(dateInMillis), TimelineProcessor.VERSION, new DateTime(dateInMillis, DateTimeZone.UTC), now)){
                    continue;  // Do not return out-dated timeline from dynamoDB.
                }

                finalResultMap.put(dateToStringMapping.get(dateInMillis), ImmutableList.copyOf(cachedData.get(dateInMillis).timeline));
            }
        }

        return ImmutableMap.copyOf(finalResultMap);
    }

    public static boolean shouldInvalidateCache(final CachedTimelines cachedTimelines, final String currentVersion,
                                                final DateTime targetDateUTC,
                                                final DateTime nowUTC){
        if(nowUTC.minusDays(20).isAfter(targetDateUTC)){
            if(cachedTimelines.isEmpty()){
                return true;   // Empty, timeline not yet computed, force re-generate.
            }

            if(cachedTimelines.version.equals(currentVersion)){
                return false;  // same version, up to date
            }

            return true;
        }

        return false;  // data too old, never invalidate, always use old timeline.
    }

    /*
    * Get events for maybe not consecutive days, internal use only
     */
    private ImmutableMap<Long, CachedTimelines> getTimelinesForDatesImpl(long accountId, final Collection<Long> datesInMillis){
        if(datesInMillis.size() > MAX_REQUEST_DAYS){
            LOGGER.warn("Request too large for events, num of days requested: {}, accountId: {}, table: {}", datesInMillis.size(), accountId, this.tableName);
            throw new RuntimeException("Request too many days event.");
        }

        final Map<Long, CachedTimelines> finalResult = new HashMap<>();
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final Long[] sortedDateMillis = datesInMillis.toArray(new Long[0]);
        Arrays.sort(sortedDateMillis);

        final Long startDateMillis = sortedDateMillis[0];
        final Long endDateMillis = sortedDateMillis[sortedDateMillis.length - 1];

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(startDateMillis)),
                        new AttributeValue().withN(String.valueOf(endDateMillis)));


        queryConditions.put(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME, selectDateCondition);

        // AND accound_id = :accound_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        Map<String, AttributeValue> lastEvaluatedKey = null;
        final Collection<String> targetAttributeSet = new HashSet<String>();
        Collections.addAll(targetAttributeSet,
                ACCOUNT_ID_ATTRIBUTE_NAME,
                TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME,
                DATA_BLOB_ATTRIBUTE_NAME,
                UPDATED_AT_ATTRIBUTE_NAME,
                COMPRESS_TYPE_ATTRIBUTE_NAME,
                VERSION);


        int loopCount = 0;

        // Loop and construct queries..
        do{
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(targetAttributeSet)
                    .withLimit(MAX_REQUEST_DAYS)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            if(queryResult.getItems() == null){
                break;
            }

            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            for(final Map<String, AttributeValue> item:items){
                if(!item.keySet().containsAll(targetAttributeSet)){
                    LOGGER.warn("Missing field in item {}", item);
                    continue;
                }

                final Long dateInMillis = Long.valueOf(item.get(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME).getN());
                final ArrayList<Timeline> eventsWithAllTypes = new ArrayList<>();

                final ByteBuffer byteBuffer = item.get(DATA_BLOB_ATTRIBUTE_NAME).getB();
                final byte[] compressed = byteBuffer.array();

                final Compression.CompressionType compressionType = Compression.CompressionType.fromInt(Integer.valueOf(item.get(COMPRESS_TYPE_ATTRIBUTE_NAME).getN()));
                final String version = item.get(VERSION).getS();

                try {
                    final byte[] decompressed = Compression.decompress(compressed, compressionType);
                    final String jsonString = new String(decompressed, JSON_CHARSET);
                    final List<Timeline> eventList = mapper.readValue(jsonString, new TypeReference<List<Timeline>>() {});
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

                final CachedTimelines cachedTimelines = CachedTimelines.create(eventsWithAllTypes, version);
                finalResult.put(dateInMillis, cachedTimelines);

            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;
        }while (lastEvaluatedKey != null && loopCount < MAX_CALL_COUNT);



        if(lastEvaluatedKey != null){
            // We still have something not fetched. Request still too large!
            LOGGER.warn("Request too large for events, num of days requested: {}, accountId: {}, tableName: {}", datesInMillis.size(), accountId, this.tableName);
            throw new RuntimeException("Request too many days event.");

        }

        // Fill the non-exist days with empty lists
        for(final Long dateInMillis:datesInMillis){
            if(!finalResult.containsKey(dateInMillis)){
                finalResult.put(dateInMillis, CachedTimelines.createEmpty());
            }
        }

        return ImmutableMap.copyOf(finalResult);
    }


    @Timed
    public void saveTimelinesForDate(long accountId, final DateTime dateOfTheNightLocalUTC, final List<Timeline> data){
        final Map<DateTime, List<Timeline>> convertedParam = new HashMap<>();
        convertedParam.put(dateOfTheNightLocalUTC, data);
        saveTimelinesForDates(accountId, convertedParam);
    }

    @Timed
    public void saveTimelinesForDates(long accountId, final Map<DateTime, List<Timeline>> data){
        final Map<Long, List<Timeline>> dataWithStringDates = new HashMap<>();

        for(final DateTime dateOfTheNightLocalUTC:data.keySet()){
            dataWithStringDates.put(dateOfTheNightLocalUTC.getMillis(), data.get(dateOfTheNightLocalUTC));
        }

        setTimelinesForDatesLong(accountId, dataWithStringDates);
    }

    private void setTimelinesForDatesLong(long accountId, final Map<Long, List<Timeline>> data){
        if(data.size() == 0){
            LOGGER.info("Empty motion data for account_id = {}", accountId);
            return;
        }

        if(data.size() > MAX_REQUEST_DAYS){
            LOGGER.error("Account: {} tries to upload large data, day count: {}", accountId, data.size());
            throw new RuntimeException("data is too large");
        }

        final ArrayList<WriteRequest> putRequests = new ArrayList<WriteRequest>();

        long currentBatchSize = 0;

        for(final Long targetDateOfNightLocalUTC:data.keySet()) {

            final List<Timeline> timelines = data.get(targetDateOfNightLocalUTC);


            try {
                final String jsonEventList = mapper.writeValueAsString(timelines);

                final HashMap<String, AttributeValue> item = new HashMap<>();
                item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
                item.put(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(targetDateOfNightLocalUTC)));
                item.put(UPDATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(DateTime.now().getMillis())));
                item.put(VERSION, new AttributeValue().withS(TimelineProcessor.VERSION));

                final int compressType = Compression.CompressionType.NONE.getValue();
                item.put(COMPRESS_TYPE_ATTRIBUTE_NAME, new AttributeValue().withN(
                        String.valueOf(compressType)));

                // final ByteBuffer byteBuffer = ByteBuffer.wrap(builder.build().toByteArray());

                final byte[] compressedData = Compression.compress(jsonEventList.getBytes(JSON_CHARSET),
                        Compression.CompressionType.fromInt(compressType));
                final ByteBuffer byteBuffer = ByteBuffer.wrap(compressedData);
                byteBuffer.position(0);

                item.put(DATA_BLOB_ATTRIBUTE_NAME, new AttributeValue().withB(byteBuffer));


                final PutRequest putItemRequest = new PutRequest()
                        .withItem(item);

                final WriteRequest writeRequest = new WriteRequest().withPutRequest(putItemRequest);

                if(currentBatchSize + 1 > MAX_BATCH_SIZE) {
                    LOGGER.info("Saving events for account_id: {}", accountId);

                    batchWrite(accountId, putRequests);
                    putRequests.clear();
                    currentBatchSize = 0;

                    LOGGER.info("Events saved for account_id: {}", accountId);
                }

                putRequests.add(writeRequest);
                currentBatchSize ++;

            }catch (JsonProcessingException jpe){
                LOGGER.error("Serialize events for account {}, night {} failed: {}",
                        accountId,
                        //type,
                        targetDateOfNightLocalUTC,
                        jpe.getMessage());
            }catch (UnsupportedEncodingException uee) {
                LOGGER.error("Serialize events for account {}, night {} failed: {}",
                        accountId,
                        //type,
                        targetDateOfNightLocalUTC,
                        uee.getMessage());
            }catch (IOException ioe){
                LOGGER.error("Compress events for account {}, night {} failed: {}",
                        accountId,
                        //type,
                        targetDateOfNightLocalUTC,
                        ioe.getMessage());
            }


        }

        if(putRequests.size() > 0){
            batchWrite(accountId, putRequests);
        }
    }

    private void batchWrite(final long accountId, final List<WriteRequest> writeRequests){
        LOGGER.info("WriteRequest number per batch: {}", writeRequests.size());

        Map<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        requestItems.put(this.tableName, writeRequests);

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
            LOGGER.error("Account: {} tries to upload large event data", accountId);
            throw new RuntimeException("data is too large");
        }


    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(TimelineDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(TimelineDAODynamoDB.TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(TimelineDAODynamoDB.ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(TimelineDAODynamoDB.TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}

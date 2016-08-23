package com.hello.suripu.coredropwizard.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

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
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.hello.suripu.core.db.util.Compression;
import com.hello.suripu.core.models.CachedTimelines;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.coredw8.timeline.InstrumentedTimelineProcessor;

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

import io.dropwizard.jackson.GuavaExtrasModule;

import static com.codahale.metrics.MetricRegistry.name;

public class TimelineDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    private final MetricRegistry metrics;
    private final Meter timelineExpiredAtRequestMeter;
    private final Meter timelineNotExpiredAtRequestMeter;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    public static final String TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME = "target_date_of_night";
    public static final String DATA_BLOB_ATTRIBUTE_NAME = "timelines";

    public static final String COMPRESS_TYPE_ATTRIBUTE_NAME = "compression_type";
    public static final String UPDATED_AT_ATTRIBUTE_NAME = "updated_at";

    public static final String VERSION = "version";
    public static final String EXPIRED_AT_MILLIS = "expired_at_millis";


    private final int MAX_CALL_COUNT = 5;
    private final int MAX_BATCH_SIZE = 25;  // Based on: http://docs.aws.amazon.com/cli/latest/reference/dynamodb/batch-write-item.html
    private final int maxBackTrackDays;

    public static final int MAX_REQUEST_DAYS = 31;

    public final String JSON_CHARSET = "UTF-8";

    private static ObjectMapper mapper = new ObjectMapper();

    public TimelineDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName, final int maxBackTrackDays, final MetricRegistry metricRegistry){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
        this.metrics = metricRegistry;

        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new GuavaExtrasModule());
        mapper.registerModule(new JodaModule());

        this.maxBackTrackDays = maxBackTrackDays;

        this.timelineExpiredAtRequestMeter = metrics.meter(name(TimelineDAODynamoDB.class, "timelines-expired-at-request"));
        this.timelineNotExpiredAtRequestMeter = metrics.meter(name(TimelineDAODynamoDB.class, "cache-hits-at-request"));
    }

    public Optional<TimelineResult> getTimelinesForDate(long accountId, final DateTime targetDateOfNightLocalUTC){
        final Collection<DateTime> convertedParam = new ArrayList<DateTime>();
        convertedParam.add(targetDateOfNightLocalUTC);
        final ImmutableMap<DateTime,TimelineResult> result = this.getTimelinesForDates(accountId, convertedParam);
        if(result.containsKey(targetDateOfNightLocalUTC)){
            return Optional.of(result.get(targetDateOfNightLocalUTC));
        }

        return Optional.absent();
    }

    private Map<DateTime, TimelineResult> filterExpiredCache(final ImmutableMap<Long, CachedTimelines> dataFromCache,
                                                                            final Map<Long, DateTime> requestDatesLocalUTCMillis,
                                                                            final DateTime now,
                                                                            final int maxInvalidateSpanInDay){
        final Map<DateTime, TimelineResult> finalResultMap = new HashMap<>();
        for(final Long timelineDateLocalUTCMillis:dataFromCache.keySet()){
            if(requestDatesLocalUTCMillis.containsKey(timelineDateLocalUTCMillis)){
                final DateTime requestDateLocalUTC = requestDatesLocalUTCMillis.get(timelineDateLocalUTCMillis);
                if(dataFromCache.get(timelineDateLocalUTCMillis).shouldInvalidate(InstrumentedTimelineProcessor.VERSION, new DateTime(timelineDateLocalUTCMillis, DateTimeZone.UTC), now, maxInvalidateSpanInDay)){
                    this.timelineExpiredAtRequestMeter.mark();
                    continue;  // Do not return out-dated timeline from dynamoDB.
                }

                finalResultMap.put(requestDateLocalUTC, dataFromCache.get(timelineDateLocalUTCMillis).result);
                this.timelineNotExpiredAtRequestMeter.mark();
            }
        }

        return ImmutableMap.copyOf(finalResultMap);
    }

    public ImmutableMap<DateTime, TimelineResult> getTimelinesForDates(long accountId, final Collection<DateTime> dates){
        final Map<Long, DateTime> requestDatesLocalUTCMillisToDateTimeMap = new HashMap<>();
        for(final DateTime date:dates){
            requestDatesLocalUTCMillisToDateTimeMap.put(date.getMillis(), date);
        }

        final Collection<Long> requestDatesLocalUTCMillis = requestDatesLocalUTCMillisToDateTimeMap.keySet();
        final ImmutableMap<Long, CachedTimelines> cachedData = this.getTimelinesForDatesImpl(accountId, requestDatesLocalUTCMillis);

        final Map<DateTime, TimelineResult> finalResultMap = filterExpiredCache(cachedData,  // Tim you are right, wrong level of abstract makes program read like shit.
                requestDatesLocalUTCMillisToDateTimeMap,
                DateTime.now(),
                this.maxBackTrackDays);
        return ImmutableMap.copyOf(finalResultMap);
    }

    public boolean setExpiredAt(final Long accountId, DateTime targetDateLocalUTC, final DateTime expiredAtUTC){

        try {
            final Map<String, ExpectedAttributeValue> putConditions = new HashMap<>();

            putConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, new ExpectedAttributeValue(
                    new AttributeValue().withN(String.valueOf(accountId))
            ));
            putConditions.put(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME, new ExpectedAttributeValue(
                    new AttributeValue().withN(String.valueOf(targetDateLocalUTC.withTimeAtStartOfDay().getMillis()))
            ));

            final HashMap<String, AttributeValueUpdate> items = new HashMap<>();

            items.put(EXPIRED_AT_MILLIS, new AttributeValueUpdate()
                    .withAction(AttributeAction.PUT)
                    .withValue(new AttributeValue().withN(String.valueOf(expiredAtUTC.getMillis()))));

            final HashMap<String, AttributeValue> keys = new HashMap<>();
            keys.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
            keys.put(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(targetDateLocalUTC.withTimeAtStartOfDay().getMillis())));


            final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(tableName)
                    .withKey(keys)
                    .withAttributeUpdates(items)
                    .withExpected(putConditions)
                    .withReturnValues(ReturnValue.ALL_NEW);

            final UpdateItemResult result = this.dynamoDBClient.updateItem(updateItemRequest);
            if(result.getAttributes().size() > 0) {

                return true;
            }
        }  catch (AmazonServiceException ase) {
            LOGGER.error("Failed to invalidate cache after for account {} and date {}, error {}",
                    accountId,
                    targetDateLocalUTC.withTimeAtStartOfDay(),
                    ase.getMessage());
        }

        return false;
    }

    public boolean invalidateCache(final Long accountId, final DateTime targetDateLocalUTC, final DateTime now){
        return setExpiredAt(accountId, targetDateLocalUTC, now.minusMinutes(1));
    }



    /*
    * Get events for maybe not consecutive days, internal use only
     */
    protected ImmutableMap<Long, CachedTimelines> getTimelinesForDatesImpl(final Long accountId, final Collection<Long> datesInMillis){
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
                EXPIRED_AT_MILLIS,
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
                final Long expiredAtMillis = Long.valueOf(item.get(EXPIRED_AT_MILLIS).getN());

                try {
                    final byte[] decompressed = Compression.decompress(compressed, compressionType);
                    final String jsonString = new String(decompressed, JSON_CHARSET);
                    final TimelineResult result = mapper.readValue(jsonString, new TypeReference<TimelineResult>() {});

                    final CachedTimelines cachedTimelines = CachedTimelines.create(result, version, expiredAtMillis);
                    finalResult.put(dateInMillis, cachedTimelines);

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
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            loopCount++;
        }while (lastEvaluatedKey != null && loopCount < MAX_CALL_COUNT);



        if(lastEvaluatedKey != null){
            // We still have something not fetched. Request still too large!
            LOGGER.warn("Request too large for events, num of days requested: {}, accountId: {}, tableName: {}", datesInMillis.size(), accountId, this.tableName);
            throw new RuntimeException("Request too many days event.");

        }

        return ImmutableMap.copyOf(finalResult);
    }


    public void saveTimelinesForDate(final Long accountId, final DateTime dateOfTheNightLocalUTC, final TimelineResult data ){
        final Map<DateTime, TimelineResult> convertedParam = new HashMap<>();
        convertedParam.put(dateOfTheNightLocalUTC, data);
        saveTimelinesForDates(accountId, convertedParam);
    }

    public void saveTimelinesForDates(final Long accountId, final Map<DateTime,TimelineResult> data){
        final Map<Long, TimelineResult> dataWithStringDates = new HashMap<>();

        for(final DateTime dateOfTheNightLocalUTC:data.keySet()){
            dataWithStringDates.put(dateOfTheNightLocalUTC.getMillis(), data.get(dateOfTheNightLocalUTC));
        }

        setTimelinesForDatesLong(accountId, dataWithStringDates);
    }

    private void setTimelinesForDatesLong(final Long accountId, final Map<Long, TimelineResult> data){
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

            final TimelineResult timelines = data.get(targetDateOfNightLocalUTC);


            try {
                final String jsonEventList = mapper.writeValueAsString(timelines);

                final HashMap<String, AttributeValue> item = new HashMap<>();
                item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
                item.put(TARGET_DATE_OF_NIGHT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(targetDateOfNightLocalUTC)));
                item.put(UPDATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(DateTime.now().getMillis())));
                item.put(VERSION, new AttributeValue().withS(InstrumentedTimelineProcessor.VERSION));
                item.put(EXPIRED_AT_MILLIS, new AttributeValue().withN(String.valueOf(DateTime.now().plusHours(3).getMillis())));

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

    private void batchWrite(final Long accountId, final List<WriteRequest> writeRequests){
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

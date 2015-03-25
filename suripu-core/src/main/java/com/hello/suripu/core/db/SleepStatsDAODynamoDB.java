package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SleepStatsDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(SleepStatsDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;
    private final String version;
    public final Set<String> targetAttributes;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id"; // hash
    public static final String DATE_ATTRIBUTE_NAME = "date"; // range

    public static final String DOW_ATTRIBUTE_NAME = "day_of_week";
    public static final String OFFSET_MILLIS_ATTRIBUTE_NAME = "offset_millis";

    // sleep score stuff
    public static final String SCORE_ATTRIBUTE_NAME = "score";
    public static final String TYPE_ATTRIBUTE_NAME = "type";
    public static final String VERSION_ATTRIBUTE_NAME = "version";

    // motion stuff
    public static final String NUM_MOTIONS_ATTRIBUTE_NAME = "num_motions";
    public static final String MOTIONS_PERIOD_MINS_ATTRIBUTE_NAME = "motion_period_mins";
    public static final String AVG_MOTION_AMPLITUDE_ATTRIBUTE_NAME = "avg_motion_amplitude";
    public static final String MAX_MOTION_AMPLITUDE_ATTRIBUTE_NAME = "max_motion_amplitude";
    public static final String MOTION_SCORE_ATTRIBUTE_NAME = "motion_score";

    // sleep stats stuff
    public static final String SLEEP_DURATION_ATTRIBUTE_NAME = "sleep_duration";
    public static final String LIGHT_SLEEP_ATTRIBUTE_NAME = "light_sleep";
    public static final String SOUND_SLEEP_ATTRIBUTE_NAME = "sound_sleep";
    public static final String ASLEEP_TIME_ATTRIBUTE_NAME = "fall_asleep_time";
    public static final String AWAKE_TIME_ATTRIBUTE_NAME = "awake_time";
    public static final String SLEEP_ONSET_ATTRIBUTE_NAME = "sleep_onset_minutes";
    public static final String SLEEP_MOTION_COUNT_ATTRIBUTE_NAME = "sleep_motion_count";

    private static final int MAX_CALL_COUNT = 5;



    public static final String DEFAULT_SCORE_TYPE = "sleep";

    public SleepStatsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName, final String version) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName + "_" + version;
        this.version = version;
        this.targetAttributes = new HashSet<>();
        Collections.addAll(targetAttributes, ACCOUNT_ID_ATTRIBUTE_NAME, DATE_ATTRIBUTE_NAME,
                DOW_ATTRIBUTE_NAME, OFFSET_MILLIS_ATTRIBUTE_NAME,
                SCORE_ATTRIBUTE_NAME, TYPE_ATTRIBUTE_NAME, VERSION_ATTRIBUTE_NAME,
                MOTION_SCORE_ATTRIBUTE_NAME,
                NUM_MOTIONS_ATTRIBUTE_NAME, MOTIONS_PERIOD_MINS_ATTRIBUTE_NAME,
                AVG_MOTION_AMPLITUDE_ATTRIBUTE_NAME, MAX_MOTION_AMPLITUDE_ATTRIBUTE_NAME,
                SLEEP_DURATION_ATTRIBUTE_NAME,
                LIGHT_SLEEP_ATTRIBUTE_NAME,
                SOUND_SLEEP_ATTRIBUTE_NAME,
                ASLEEP_TIME_ATTRIBUTE_NAME,
                AWAKE_TIME_ATTRIBUTE_NAME,
                SLEEP_ONSET_ATTRIBUTE_NAME,
                SLEEP_MOTION_COUNT_ATTRIBUTE_NAME);
    }

    @Timed public Boolean updateStat(final Long accountId, final DateTime date, final Integer sleepScore, final MotionScore motionScore, final SleepStats stats, final Integer offsetMillis) {
        LOGGER.debug("Write single score: {}, {}, {}", accountId, date, sleepScore);

        final String dateString = DateTimeUtil.dateToYmdString(date.withTimeAtStartOfDay());

        try {
            final HashMap<String, AttributeValueUpdate> item = this.createUpdateItem(accountId, date, sleepScore, motionScore, stats, offsetMillis);

            final Map<String, AttributeValue> key = new HashMap<>();
            key.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
            key.put(DATE_ATTRIBUTE_NAME, new AttributeValue().withS(dateString));

//            final Map<String, ExpectedAttributeValue> putConditions = new HashMap<>();
//            putConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, new ExpectedAttributeValue(
//                    new AttributeValue().withN(String.valueOf(accountId))));
//            putConditions.put(DATE_ATTRIBUTE_NAME, new ExpectedAttributeValue(
//                    new AttributeValue().withS(dateString)));


            final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(this.tableName)
                    .withKey(key)
                    .withAttributeUpdates(item)
                    .withReturnValues(ReturnValue.ALL_NEW);

//                    .withExpected(putConditions)

            final UpdateItemResult result = this.dynamoDBClient.updateItem(updateItemRequest);
            if (result.getAttributes().size() > 0) {
                return true;
            }
        } catch (AmazonServiceException ase) {
            LOGGER.error("Failed to update sleep score for account {}, date {}, score {}",
                    accountId, date, sleepScore);
        }
        return false;

    }

    @Timed
    public Optional<AggregateSleepStats> getSingleStat(final Long accountId, final String date) {

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
            return Optional.absent();
        }

        if(!item.keySet().containsAll(this.targetAttributes)){
            LOGGER.warn("Missing field in item {}", item);
            return Optional.absent();
        }

        return Optional.of(this.createAggregateStat(item));

    }

    @Timed
    public ImmutableList<AggregateSleepStats> getBatchStats(final Long accountId, final String startDate, final String endDate, final int numDays){

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

        final List<AggregateSleepStats> scoreResults = new ArrayList<>();

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
                    final AggregateSleepStats score = this.createAggregateStat(item);
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
        ArrayList<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType("N"));
        attributes.add(new AttributeDefinition().withAttributeName(DATE_ATTRIBUTE_NAME).withAttributeType("S"));

        // keys
        ArrayList<KeySchemaElement> keySchema = new ArrayList<>();
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

    private HashMap<String, AttributeValue> createItem(final Long accountId, final DateTime date, final Integer sleepScore,
                                                       final MotionScore motionScore,
                                                       final SleepStats stats, final Integer offsetMillis) {
        final HashMap<String, AttributeValue> item = new HashMap<>();

        // Hash
        item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));

        // range
        final String dateString = DateTimeUtil.dateToYmdString(date.withTimeAtStartOfDay());
        item.put(DATE_ATTRIBUTE_NAME, new AttributeValue().withS(dateString));

        final int dayOfWeek = date.getDayOfWeek();
        item.put(DOW_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(dayOfWeek)));
        item.put(OFFSET_MILLIS_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(offsetMillis)));

        //score stuff
        item.put(SCORE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(sleepScore)));
        item.put(TYPE_ATTRIBUTE_NAME, new AttributeValue().withS(DEFAULT_SCORE_TYPE));
        item.put(VERSION_ATTRIBUTE_NAME, new AttributeValue().withS(this.version));

        // motion stuff
        item.put(MOTION_SCORE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(motionScore.score)));
        item.put(NUM_MOTIONS_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(motionScore.numMotions)));
        item.put(MOTIONS_PERIOD_MINS_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(motionScore.motionPeriodMinutes)));
        item.put(AVG_MOTION_AMPLITUDE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(motionScore.avgAmplitude)));
        item.put(MAX_MOTION_AMPLITUDE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(motionScore.maxAmplitude)));

        // stats stuff
        item.put(SLEEP_DURATION_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(stats.sleepDurationInMinutes)));
        item.put(LIGHT_SLEEP_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(stats.lightSleepDurationInMinutes)));
        item.put(SOUND_SLEEP_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(stats.soundSleepDurationInMinutes)));
        item.put(ASLEEP_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(String.valueOf(stats.sleepTime)));
        item.put(AWAKE_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(String.valueOf(stats.wakeTime)));
        item.put(SLEEP_ONSET_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(stats.sleepOnsetTimeMinutes)));
        item.put(SLEEP_MOTION_COUNT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(stats.numberOfMotionEvents)));

        return item;
    }

    private HashMap<String, AttributeValueUpdate> createUpdateItem(final Long accountId, final DateTime date, final Integer sleepScore,
                                                                   final MotionScore motionScore,
                                                                   final SleepStats stats,
                                                                   final Integer offsetMillis) {
        final HashMap<String, AttributeValueUpdate> item = new HashMap<>();

        final HashMap<String, AttributeValue> values = createItem(accountId, date, sleepScore, motionScore, stats, offsetMillis);
        for (final String attribute : values.keySet()) {
            if (attribute.equals(ACCOUNT_ID_ATTRIBUTE_NAME) || attribute.equals(DATE_ATTRIBUTE_NAME)) {
                continue;
            }

            item.put(attribute,new AttributeValueUpdate().withAction(AttributeAction.PUT)
                    .withValue(values.get(attribute)));
        }

        return item;
    }

    private AggregateSleepStats createAggregateStat(Map<String, AttributeValue> item) {
        final Long accountId = Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN());
        final Integer offsetMillis = Integer.valueOf(item.get(OFFSET_MILLIS_ATTRIBUTE_NAME).getN());

        final Integer score = Integer.valueOf(item.get(SCORE_ATTRIBUTE_NAME).getN());
        final String version = item.get(VERSION_ATTRIBUTE_NAME).getS();

        final MotionScore motionScore = new MotionScore(
                Integer.valueOf(item.get(NUM_MOTIONS_ATTRIBUTE_NAME).getN()),
                Integer.valueOf(item.get(MOTIONS_PERIOD_MINS_ATTRIBUTE_NAME).getN()),
                Float.valueOf(item.get(AVG_MOTION_AMPLITUDE_ATTRIBUTE_NAME).getN()),
                Integer.valueOf(item.get(MAX_MOTION_AMPLITUDE_ATTRIBUTE_NAME).getN()),
                Integer.valueOf(item.get(MOTION_SCORE_ATTRIBUTE_NAME).getN())
        );


        final SleepStats stats = new SleepStats(
                Integer.valueOf(item.get(SOUND_SLEEP_ATTRIBUTE_NAME).getN()),
                Integer.valueOf(item.get(LIGHT_SLEEP_ATTRIBUTE_NAME).getN()),
                Integer.valueOf(item.get(SLEEP_DURATION_ATTRIBUTE_NAME).getN()),
                Integer.valueOf(item.get(SLEEP_MOTION_COUNT_ATTRIBUTE_NAME).getN()),
                Long.valueOf(item.get(ASLEEP_TIME_ATTRIBUTE_NAME).getS()),
                Long.valueOf(item.get(AWAKE_TIME_ATTRIBUTE_NAME).getS()),
                Integer.valueOf(item.get(SLEEP_ONSET_ATTRIBUTE_NAME).getN())
        );

        final DateTime date = DateTimeUtil.ymdStringToDateTime(item.get(DATE_ATTRIBUTE_NAME).getS());

        return new AggregateSleepStats(accountId, date, offsetMillis, score, version, motionScore, stats);
    }

}

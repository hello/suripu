package com.hello.suripu.core.db;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hello.suripu.core.db.dynamo.Expressions;
import com.hello.suripu.core.db.dynamo.expressions.Expression;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.TimelineError;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jarredheinrich on 2/7/17.
 */
public class MainEventTimesDynamoDB extends TimeSeriesDAODynamoDB<MainEventTimes> implements MainEventTimesDAO{

    private final static Logger LOGGER = LoggerFactory.getLogger(MainEventTimesDynamoDB.class);

    public enum Attribute implements com.hello.suripu.core.db.dynamo.Attribute {
        ACCOUNT_ID("account", "N"), // Hash key
        SLEEP_PERIOD("sleep_period", "S"),
        DATE_SLEEP_PERIOD("date|sleep_period", "S"), // Range key // <utc_timestamp>|<external_device_id>
        DATE("date", "S"),
        CREATED_AT_TIME("created_at_time", "S"),
        CREATED_AT_OFFSET("created_at_offset", "S"),
        IN_BED_TIME("in_bed_time", "S"),
        IN_BED_OFFSET("in_bed_offset", "S"),
        SLEEP_TIME("sleep_time", "S"),
        SLEEP_OFFSET("sleep_offset", "S"),
        WAKE_UP_TIME("wake_up_time", "S"),
        WAKE_UP_OFFSET("wake_up_offset", "S"),
        OUT_OF_BED_TIME("out_of_bed_time", "S"),
        OUT_OF_BED_OFFSET("out_of_bed_offset", "S"),
        ALGORITHM_TYPE("algorithm_type", "N"),
        TIMELINE_ERROR("timeline_error", "N")
        ;

        private final String name;
        private final String type;

        Attribute(final String name, final String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String shortName() {
            return name;
        }

        @Override
        public String sanitizedName() {
            return toString();
        }

        @Override
        public String type() {
            return type;
        }


        AttributeValue get(final Map<String, AttributeValue> item) {
            return item.get(this.shortName());
        }

        String getString(final Map<String, AttributeValue> item) {
            final AttributeValue av = get(item);
            if (av == null) {
                return "";
            }
            return av.getS();
        }

        Long getLong(final Map<String, AttributeValue> item) {
            final AttributeValue av = get(item);
            if (av == null) {
                return null;
            }
            return Long.valueOf(av.getN());
        }

        Integer getInteger(final Map<String, AttributeValue> item) {
            final AttributeValue av = get(item);
            if (av == null) {
                return null;
            }
            return Integer.valueOf(av.getN());
        }

        Optional<Integer> getOptionalInteger(final Map<String, AttributeValue> item) {
            final AttributeValue av = get(item);
            if (av == null) {
                return Optional.absent();
            }
            return Optional.of(Integer.valueOf(av.getN()));
        }

        DateTime getDateTime(final Map<String, AttributeValue> item) {
            final Long l = getLong(item);
            if (l == null) {
                return null;
            }
            return new DateTime(l, DateTimeZone.UTC);
        }

    }

    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);

    public MainEventTimesDynamoDB(final AmazonDynamoDB client, final String tablePrefix) {
        super(client, tablePrefix);
    }

    //region TimeSeriesDAODynamoDB
    @Override
    protected Logger logger() {
        return LOGGER;
    }

    @Override
    protected Integer maxQueryAttempts() {
        return 5;
    }

    @Override
    protected Integer maxBatchWriteAttempts() {
        return 5;
    }

    @Override
    protected String hashKeyName() {
        return MainEventTimesDynamoDB.Attribute.ACCOUNT_ID.shortName();
    }

    @Override
    protected String rangeKeyName() {
        return Attribute.DATE_SLEEP_PERIOD.shortName();
    }

    @Override
    protected String hashKeyType() {
        return MainEventTimesDynamoDB.Attribute.ACCOUNT_ID.type();
    }

    @Override
    protected String rangeKeyType() {
        return Attribute.DATE_SLEEP_PERIOD.type();
    }

    @Override
    protected String getHashKey(final AttributeValue attributeValue) {
        return attributeValue.getN();
    }

    @Override
    protected String getRangeKey(final AttributeValue attributeValue) {
        return attributeValue.getS();
    }

    @Override
    protected DateTime getTimestamp(final MainEventTimes model) {
        return model.sleepPeriod.targetDate;
    }

    @Override
    protected Map<String, AttributeValue> toAttributeMap(final MainEventTimes model) {
        final ImmutableMap.Builder<String, AttributeValue> builder = ImmutableMap.builder();
        builder.put(MainEventTimesDynamoDB.Attribute.ACCOUNT_ID.shortName(), toAttributeValue(model.accountId))
                .put(Attribute.DATE_SLEEP_PERIOD.shortName(), getRangeKey(model.sleepPeriod.targetDate, Optional.of(model.sleepPeriod.period)))
                .put(Attribute.SLEEP_PERIOD.shortName(), toAttributeValue(model.sleepPeriod.period.name()))
                .put(Attribute.DATE.shortName(), toAttributeValue(model.sleepPeriod.targetDate))
                .put(Attribute.CREATED_AT_TIME.shortName(), toAttributeValue(model.createdAt.time))
                .put(Attribute.CREATED_AT_OFFSET.shortName(), toAttributeValue(model.createdAt.offset))

                .put(Attribute.IN_BED_TIME.shortName(), toAttributeValue(model.eventTimeMap.get(Event.Type.IN_BED).time))
                .put(Attribute.IN_BED_OFFSET.shortName(), toAttributeValue(model.eventTimeMap.get(Event.Type.IN_BED).offset))

                .put(Attribute.SLEEP_TIME.shortName(), toAttributeValue(model.eventTimeMap.get(Event.Type.SLEEP).time))
                .put(Attribute.SLEEP_OFFSET.shortName(), toAttributeValue(model.eventTimeMap.get(Event.Type.SLEEP).offset))

                .put(Attribute.WAKE_UP_TIME.shortName(), toAttributeValue(model.eventTimeMap.get(Event.Type.WAKE_UP).time))
                .put(Attribute.WAKE_UP_OFFSET.shortName(), toAttributeValue(model.eventTimeMap.get(Event.Type.WAKE_UP).offset))

                .put(Attribute.OUT_OF_BED_TIME.shortName(), toAttributeValue(model.eventTimeMap.get(Event.Type.OUT_OF_BED).time))
                .put(Attribute.OUT_OF_BED_OFFSET.shortName(), toAttributeValue(model.eventTimeMap.get(Event.Type.OUT_OF_BED).offset))
                .put(Attribute.ALGORITHM_TYPE.shortName(), toAttributeValue(model.algorithmType.getValue()))
                .put(Attribute.TIMELINE_ERROR.shortName(), toAttributeValue(model.timelineError.getValue()));

        return builder.build();
    }

    @Override
    public String getTableName(final DateTime dateTime) {
        // Yearly tables
        return tablePrefix;
    }

    @Override
    public List<String> getTableNames(final DateTime start, final DateTime end) {
        // Yearly tables
        final List<DateTime> dateTimes = DateTimeUtil.dateTimesForStartOfYearBetweenDates(start, end);
        final List<String> names = new ArrayList<>(dateTimes.size());
        for (final DateTime dateTime: dateTimes) {
            final String tableName = getTableName(dateTime);
            if (!names.contains(tableName)) { // O(n) but if you're worried about that you don't understand this class.
                names.add(tableName);
            }
        }
        return names;
    }
    //endregion TimeSeriesDAODynamoDB

    //region write
    public Boolean insert(final MainEventTimes mainEventTimes) {


        int numTries = 0;
        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(getTableName(mainEventTimes.sleepPeriod.targetDate))
                .withItem(toAttributeMap(mainEventTimes));

        do {
            try {
                dynamoDBClient.putItem(putItemRequest);
                return true;
            } catch (ProvisionedThroughputExceededException ptee) {
                LOGGER.error("error=ProvisionedThroughputExceededException account_id={}", mainEventTimes.accountId);
            } catch (InternalServerErrorException isee) {
                LOGGER.error("error=InternalServerErrorException account_id={}", mainEventTimes.accountId);
            }
            backoff(numTries);
            numTries++;
        } while (numTries < maxBatchWriteAttempts());
        return false;
    }
    //endregion write


    //region query
    private Expression getKeyConditionExpression(final Long accountId, final DateTime start, final DateTime end, final Optional<SleepPeriod.Period> period) {
        return Expressions.and(
                Expressions.equals(MainEventTimesDynamoDB.Attribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(Attribute.DATE_SLEEP_PERIOD, getRangeKey(start, period), getRangeKey(end, period))
        );
    }

    public Response<List<MainEventTimes>> query(final Long accountId, final DateTime start, final DateTime end, final SleepPeriod.Period sleepPeriod) {
        final Expression keyConditionExpression = getKeyConditionExpression(accountId, start, end, Optional.of(sleepPeriod));
        final Expression filterExpression = Expressions.equals(Attribute.SLEEP_PERIOD, new AttributeValue().withS(sleepPeriod.name()));
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(start, end), keyConditionExpression, filterExpression, ImmutableSet.copyOf(MainEventTimesDynamoDB.Attribute.values()));

        final List<MainEventTimes> sleepPeriodMainEventTimesMap = toMainEventTimesList(response.data);
        return Response.into(sleepPeriodMainEventTimesMap, response);
    }

    public Response<List<MainEventTimes>> query(final Long accountId, final DateTime start, final DateTime end) {
        final Expression keyConditionExpression = getKeyConditionExpression(accountId, start, end, Optional.absent());
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(start, end), keyConditionExpression, ImmutableSet.copyOf(MainEventTimesDynamoDB.Attribute.values()));

        final List<MainEventTimes> events = toMainEventTimesList(response.data);
        return Response.into(events, response);
    }

    /**
     * Overridden to ensure consistent reads.
     */
    @Override
    protected Response<List<Map<String, AttributeValue>>> query(final QueryRequest originalQueryRequest) {
        final QueryRequest consistentQueryRequest = originalQueryRequest.clone().withConsistentRead(true);
        return super.query(consistentQueryRequest);
    }
    //endregion query


    //region private helpers
    private static AttributeValue toAttributeValue(final String s) {
        return new AttributeValue().withS(s);
    }

    private static AttributeValue toAttributeValue(final Long l) {
        return new AttributeValue().withN(l.toString());
    }

    private static AttributeValue toAttributeValue(final Integer i) {
        return new AttributeValue().withN(i.toString());
    }


    private static AttributeValue toAttributeValue(final DateTime dt) {
        return toAttributeValue(dt.getMillis());
    }

    private static AttributeValue getRangeKey(final DateTime dateTime, final Optional<SleepPeriod.Period> type) {
        if (type.isPresent()) {
            return getRangeKey(dateTime, type.get().shortName());
        }
        return getRangeKey(dateTime, "");
    }

    private static AttributeValue getRangeKey(final DateTime dateTime, final String type) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + type);
    }

    private static MainEventTimes toMainEventTimes(final Map<String, AttributeValue> item) {
        final long accountId = Attribute.ACCOUNT_ID.getLong(item);


        final ImmutableMap<Event.Type, MainEventTimes.EventTime > eventTimeMap = ImmutableMap.<Event.Type, MainEventTimes.EventTime>builder()
                .put(Event.Type.IN_BED, new MainEventTimes.EventTime(Attribute.IN_BED_TIME.getLong(item), Attribute.IN_BED_OFFSET.getInteger(item)))
                .put(Event.Type.SLEEP, new MainEventTimes.EventTime(Attribute.SLEEP_TIME.getLong(item), Attribute.SLEEP_OFFSET.getInteger(item)))
                .put(Event.Type.WAKE_UP, new MainEventTimes.EventTime(Attribute.WAKE_UP_TIME.getLong(item), Attribute.WAKE_UP_OFFSET.getInteger(item)))
                .put(Event.Type.OUT_OF_BED, new MainEventTimes.EventTime(Attribute.OUT_OF_BED_TIME.getLong(item), Attribute.OUT_OF_BED_OFFSET.getInteger(item)))
                .build();
        final SleepPeriod.Period period = SleepPeriod.Period.fromString(Attribute.SLEEP_PERIOD.getString(item));
        final DateTime targetDate = Attribute.DATE.getDateTime(item);

        final Optional<Integer> algTypeValueOptional = Attribute.ALGORITHM_TYPE.getOptionalInteger(item);
        final Optional<Integer> timelineErrorValueOptional = Attribute.TIMELINE_ERROR.getOptionalInteger(item);

        final AlgorithmType algorithmType;
        if(algTypeValueOptional.isPresent()) {
            algorithmType = AlgorithmType.fromInteger(algTypeValueOptional.get());
        } else{
            LOGGER.warn("msg=main-event-times-missing-alg-type account_id={} sleep_period={} date={}", accountId, period, targetDate);
            algorithmType = AlgorithmType.NONE;
        }

        final TimelineError timelineError;
        if (timelineErrorValueOptional.isPresent()){
            timelineError = TimelineError.fromInteger(timelineErrorValueOptional.get());
        } else {
            LOGGER.warn("msg=main-event-times-missing-timeline-error account_id={} sleep_period={} date={}", accountId, period, targetDate);

            timelineError = TimelineError.NO_ERROR;
        }

        final MainEventTimes mainEventTimes= MainEventTimes.createMainEventTimes(
                accountId,
                SleepPeriod.createSleepPeriod(period, targetDate),
                Attribute.CREATED_AT_TIME.getLong(item),
                Attribute.CREATED_AT_OFFSET.getInteger(item),
                eventTimeMap,
                algorithmType,
                timelineError
        );
        return mainEventTimes;

    }

    private static Map<SleepPeriod.Period, MainEventTimes> toSleepPeriodMainEventTimesMap(final List<Map<String, AttributeValue>> items) {
        final Map<SleepPeriod.Period, MainEventTimes> sleepPeriodMainEventTimeMap= new HashMap<>();
        for (final Map<String, AttributeValue> item : items) {
            final MainEventTimes mainEventTimes = toMainEventTimes(item);
            sleepPeriodMainEventTimeMap.put(mainEventTimes.sleepPeriod.period, mainEventTimes);
        }
        return sleepPeriodMainEventTimeMap;
    }

    private static List<MainEventTimes> toMainEventTimesList(final List<Map<String, AttributeValue>> items) {
        final List<MainEventTimes> mainEventTimesList= new ArrayList<>(items.size());
        for (final Map<String, AttributeValue> item : items) {
            mainEventTimesList.add(toMainEventTimes(item));
        }

        return mainEventTimesList;
    }

    //endregion private helpers

    public boolean updateEventTimes(MainEventTimes mainEventTimes){
        return insert(mainEventTimes);

    }

    public List<MainEventTimes> getEventTimes(Long accountId, DateTime date){
        return query(accountId, date, date.plusDays(1)).data;
    }

    public Optional<MainEventTimes> getEventTimesForSleepPeriod(Long accountId, DateTime date, SleepPeriod.Period period){
        final List<MainEventTimes> mainEventTimesList = query(accountId, date, date.plusDays(1)).data;
        for(MainEventTimes mainEventTimes : mainEventTimesList){
            if(mainEventTimes.sleepPeriod.period == period){
                return Optional.of(mainEventTimes);
            }
        }
        return Optional.absent();
    }

}

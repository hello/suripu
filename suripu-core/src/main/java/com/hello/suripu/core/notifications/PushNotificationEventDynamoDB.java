package com.hello.suripu.core.notifications;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hello.suripu.core.db.TimeSeriesDAODynamoDB;
import com.hello.suripu.core.db.dynamo.Expressions;
import com.hello.suripu.core.db.dynamo.expressions.Expression;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 5/3/16.
 *
 * Hash key: Account ID
 * Range Key: UTC timestamp
 *
 * This class is for storing push notification events that we send to users.
 * We want this for a couple of reasons:
 *  1. Auditing purposes (what did we send)
 *  2. (Most importantly) Avoiding spamming, so we can have logic like "If user hasn't received Insight notification in 3 days..."
 *
 * Because there's no guarantee on the amount of notifications we will send, we avoid an ever-growing table by using yearly shards.
 */
public class PushNotificationEventDynamoDB extends TimeSeriesDAODynamoDB<PushNotificationEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PushNotificationEventDynamoDB.class);

    public enum Attribute implements com.hello.suripu.core.db.dynamo.Attribute {
        ACCOUNT_ID("account", "N"), // Hash key
        TYPE("type", "S"),
        TIMESTAMP_TYPE("ts|type", "S"), // Range key // <utc_timestamp>|<external_device_id>
        TIMESTAMP("timestamp", "N"),
        BODY("body", "S"),
        TARGET("target", "S"),
        DETAILS("details", "S"),
        SENSE_ID("sense_id", "S");

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

        DateTime getDateTime(final Map<String, AttributeValue> item) {
            final Long l = getLong(item);
            if (l == null) {
                return null;
            }
            return new DateTime(l, DateTimeZone.UTC);
        }
    }

    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd"; // limiting to once a day
    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);

    public PushNotificationEventDynamoDB(final AmazonDynamoDB client, final String tablePrefix) {
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
        return Attribute.ACCOUNT_ID.shortName();
    }

    @Override
    protected String rangeKeyName() {
        return Attribute.TIMESTAMP_TYPE.shortName();
    }

    @Override
    protected String hashKeyType() {
        return Attribute.ACCOUNT_ID.type();
    }

    @Override
    protected String rangeKeyType() {
        return Attribute.TIMESTAMP_TYPE.type();
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
    protected DateTime getTimestamp(final PushNotificationEvent model) {
        return model.timestamp;
    }

    @Override
    protected Map<String, AttributeValue> toAttributeMap(final PushNotificationEvent model) {
        final ImmutableMap.Builder<String, AttributeValue> builder = ImmutableMap.builder();
        builder.put(Attribute.ACCOUNT_ID.shortName(), toAttributeValue(model.accountId))
                .put(Attribute.TIMESTAMP_TYPE.shortName(), getRangeKey(new DateTime(model.timestamp, model.timeZone), Optional.of(model.type)))
                .put(Attribute.TYPE.shortName(), toAttributeValue(model.type.shortName()))
                .put(Attribute.TIMESTAMP.shortName(), toAttributeValue(model.timestamp))
                .put(Attribute.BODY.shortName(), toAttributeValue(model.helloPushMessage.body))
                .put(Attribute.TARGET.shortName(), toAttributeValue(model.helloPushMessage.target))
                .put(Attribute.DETAILS.shortName(), toAttributeValue(model.helloPushMessage.details));

        if (model.senseId.isPresent()) {
            // Optional fields are optional lol
            builder.put(Attribute.SENSE_ID.shortName(), toAttributeValue(model.senseId.get()));
        }
        return builder.build();
    }

    @Override
    public String getTableName(final DateTime dateTime) {
        // Yearly tables
        return tablePrefix + "_" + dateTime.getYear();
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
    public Boolean insert(final PushNotificationEvent event) {


        int numTries = 0;
        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(getTableName(event.timestamp))
                .withItem(toAttributeMap(event))
                // Ensure that the item does not exist. If it exists, this throws a ConditionalCheckFailedException.
                .withExpected(ImmutableMap.of(Attribute.TIMESTAMP_TYPE.shortName(), new ExpectedAttributeValue(false)));
        do {
            try {
                dynamoDBClient.putItem(putItemRequest);
                return true;
            } catch (ProvisionedThroughputExceededException ptee) {
                LOGGER.error("error=ProvisionedThroughputExceededException account_id={}", event.accountId);
            } catch (InternalServerErrorException isee) {
                LOGGER.error("error=InternalServerErrorException account_id={}", event.accountId);
            } catch (ConditionalCheckFailedException ccfe) {
                // The item already exists or we already have an item with this timestamp / account ID!
                LOGGER.warn("warn=item-already-exists account_id={} timestamp={}",
                        event.accountId, event.timestamp);
                return false;
            }
            backoff(numTries);
            numTries++;
        } while (numTries < maxBatchWriteAttempts());
        return false;
    }
    //endregion write


    //region query
    private Expression getKeyConditionExpression(final Long accountId, final DateTime start, final DateTime end, final Optional<PushNotificationEventType> type) {
        return Expressions.and(
                Expressions.equals(Attribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(Attribute.TIMESTAMP_TYPE, getRangeKey(start, type), getRangeKey(end, type))
        );
    }

    public Response<List<PushNotificationEvent>> query(final Long accountId, final DateTime start, final DateTime end, final PushNotificationEventType type) {
        final Expression keyConditionExpression = getKeyConditionExpression(accountId, start, end, Optional.of(type));
        final Expression filterExpression = Expressions.equals(Attribute.TYPE, new AttributeValue().withS(type.shortName()));
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(start, end), keyConditionExpression, filterExpression, ImmutableSet.copyOf(Attribute.values()));

        final List<PushNotificationEvent> events = toPushNotificationEventList(response.data);
        return Response.into(events, response);
    }

    public Response<List<PushNotificationEvent>> query(final Long accountId, final DateTime start, final DateTime end) {
        final Expression keyConditionExpression = getKeyConditionExpression(accountId, start, end, Optional.absent());
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(start, end), keyConditionExpression, ImmutableSet.copyOf(Attribute.values()));

        final List<PushNotificationEvent> events = toPushNotificationEventList(response.data);
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

    private static AttributeValue toAttributeValue(final DateTime dt) {
        return toAttributeValue(dt.getMillis());
    }

    private static AttributeValue getRangeKey(final DateTime dateTime, final Optional<PushNotificationEventType> type) {
        if (type.isPresent()) {
            return getRangeKey(dateTime, type.get().shortName());
        }
        return getRangeKey(dateTime, "");
    }

    private static AttributeValue getRangeKey(final DateTime dateTime, final String type) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + type);
    }

    private static PushNotificationEvent toPushNotificationEvent(final Map<String, AttributeValue> item) {
        final HelloPushMessage helloPushMessage = new HelloPushMessage(
                Attribute.BODY.getString(item),
                Attribute.TARGET.getString(item),
                Attribute.DETAILS.getString(item));
        return PushNotificationEvent.newBuilder()
                .withAccountId(Attribute.ACCOUNT_ID.getLong(item))
                .withType(PushNotificationEventType.fromString(Attribute.TYPE.getString(item)))
                .withTimestamp(Attribute.TIMESTAMP.getDateTime(item))
                .withHelloPushMessage(helloPushMessage)
                .withSenseId(Attribute.SENSE_ID.getString(item))
                .build();
    }

    private static List<PushNotificationEvent> toPushNotificationEventList(final List<Map<String, AttributeValue>> items) {
        final List<PushNotificationEvent> events = new ArrayList<>(items.size());
        for (final Map<String, AttributeValue> item : items) {
            events.add(toPushNotificationEvent(item));
        }
        return events;
    }
    //endregion private helpers

}

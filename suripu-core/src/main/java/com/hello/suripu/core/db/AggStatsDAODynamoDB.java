package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceId;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jyfan on 7/1/16.
 *
 * Schema:
 * account_id(HK)  |  date_local|external_device_id(RK)  |  avg_daily_temp  |  (rest of the values...)
 *
 * The hash key is the account_id, so it is required for getting device data and all data is attached to an account.
 *
 * The range key is the date, concatenated with the (external) device id in order to ensure uniqueness of
 * hash key + range key if you are paired to multiple Senses.
 *
 * We define a day as a roughly 24-hour period of daytime hours plus the immediately preceding hours of sleep at night.
 * Example day ranges [2016-07-01 12noon, 2016-07-02 12noon) referred to by the date "2016-07-01"
 *
 * Example range key: "2016-07-01|ABCDEF"
 *
 */
public class AggStatsDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(AggStatsDAODynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;
    private final String version;
    public final Set<String> mustHaveAttributes;

    private static final String RANGE_KEY_DATE_TEMPLATE = "yyyy-MM-dd";
    private static final DateTimeFormatter RANGE_KEY_DATE_FORMATTER = DateTimeFormat.forPattern(RANGE_KEY_DATE_TEMPLATE);

    public enum AggStatsAttribute implements Attribute {
        ACCOUNT_ID ("aid", "N"),
        RANGE_KEY ("datel|dev", "S"),  // <local_date>|<external_device_id>
        DAY_OF_WEEK ("dow", "N"),
        AVG_DAILY_TEMP ("adtemp", "N"),
        MAX_DAILY_TEMP("maxdtemp", "N"),
        MIN_DAILY_TEMP("mindtemp", "N"),
        AVG_DAILY_HUMID("adhumi", "N"),
        AVG_DAILY_RAW_DUST("addust", "N");

        private final String name;
        private final String type;

        AggStatsAttribute(String name, String type) {
            this.name = name;
            this.type = type;
        }

        private AttributeValue get(final Map<String, AttributeValue> item) {
            return item.get(this.name);
        }

        public String sanitizedName() {
            return toString();
        }

        public String shortName() {
            return name;
        }

        @Override
        public String type() {
            return type;
        }
    }

    public AggStatsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName, final String version) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName + "_" + version;
        this.version = version;
        this.mustHaveAttributes = new HashSet<>();
    }

    private static AttributeValue getRangeKey(final DateTime dateTime, final String senseId) {
        return new AttributeValue(dateTime.toString(RANGE_KEY_DATE_FORMATTER) + "|" + senseId);
    }

    private static AttributeValueUpdate toAttributeUpdateValue(final Integer value) {
        final AttributeValue attributeValue = new AttributeValue().withN(String.valueOf(value));
        return new AttributeValueUpdate().withAction(AttributeAction.PUT).withValue(attributeValue);
    }

    private static AttributeValue toAttributeValue(final Long value) {
        return new AttributeValue().withN(String.valueOf(value));
    }

    private HashMap<String, AttributeValue> createKey(final AggStats aggStats, final DeviceId deviceId) {
        final HashMap<String, AttributeValue> key = new HashMap<>();

        key.put(AggStatsAttribute.ACCOUNT_ID.name, toAttributeValue(aggStats.accountId));
        key.put(AggStatsAttribute.RANGE_KEY.name, getRangeKey(aggStats.dateLocal, deviceId.externalDeviceId.toString() ));

        return key;
    }

    private HashMap<String, AttributeValueUpdate> createItem(final AggStats aggStats, final DateTime createdTimeStampUTC) {
        final HashMap<String, AttributeValueUpdate> item = new HashMap<>();

        item.put(AggStatsAttribute.DAY_OF_WEEK.name, toAttributeUpdateValue(aggStats.dateLocal.getDayOfWeek()));
        item.put(AggStatsAttribute.AVG_DAILY_TEMP.name, toAttributeUpdateValue(aggStats.avgDailyTemp));
        item.put(AggStatsAttribute.MAX_DAILY_TEMP.name, toAttributeUpdateValue(aggStats.maxDailyTemp));
        item.put(AggStatsAttribute.MIN_DAILY_TEMP.name, toAttributeUpdateValue(aggStats.minDailyTemp));
        item.put(AggStatsAttribute.AVG_DAILY_HUMID.name, toAttributeUpdateValue(aggStats.avgDailyHumidity));
        item.put(AggStatsAttribute.AVG_DAILY_RAW_DUST.name, toAttributeUpdateValue(aggStats.avgDailyRawDust));

        return item;
    }

    public Boolean saveStat(final AggStats aggStats, final DeviceAccountPair deviceAccountPair, final DateTime createdTimeStampUTC) {
        final DeviceId deviceId = DeviceId.create(deviceAccountPair.externalDeviceId);

        try {
            //Form updateItemRequest
            final HashMap<String, AttributeValue> key = createKey(aggStats, deviceId);
            final HashMap<String, AttributeValueUpdate> item = createItem(aggStats, createdTimeStampUTC);

            final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(this.tableName)
                    .withKey(key)
                    .withAttributeUpdates(item)
                    .withReturnValues(ReturnValue.ALL_NEW);

            //Perform insert
            final UpdateItemResult updateItemResult = this.dynamoDBClient.updateItem(updateItemRequest); //putItem overwrites entire item, whereas updateItem allows to modify individual attribute when item already exists
            if (updateItemResult.getAttributes().size() > 0) {
                LOGGER.debug("");
                return Boolean.TRUE;
            }

        } catch (AmazonServiceException ase) {
            LOGGER.error("");
        }

        return Boolean.FALSE;
    }

    public Optional<AggStats> getSingleStat(final Long accountId, final DateTime dateLocal) {
        return Optional.absent();
    }

    public ImmutableList<AggStats> getBatchStats(final Long accountId, final DateTime startDateLocal, final DateTime endDateLocal) {
        return ImmutableList.copyOf( new ArrayList<AggStats>() );
    }
}

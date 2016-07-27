package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Expressions;
import com.hello.suripu.core.db.dynamo.expressions.Expression;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.AggStats;
import com.hello.suripu.core.models.DeviceId;

import com.hello.suripu.core.models.Insights.SumLengthData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

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
 * We define a day as a roughly 24-hour period of daytime hours plus the immediately following hours of sleep at night.
 * Example day ranges [2016-07-01 12noon, 2016-07-02 12noon) referred to by the date "2016-07-01"
 *
 * Example range key: "2016-07-01|ABCDEF"
 *
 */
public class AggStatsDAODynamoDB extends TimeSeriesDAODynamoDB<AggStats> {

    private final static Logger LOGGER = LoggerFactory.getLogger(AggStatsDAODynamoDB.class);

    private final String tableName;
    private final String version;

    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd";
    private static final String RANGE_KEY_DATE_TEMPLATE = "yyyy-MM-dd";
    private static final DateTimeFormatter RANGE_KEY_DATE_FORMATTER = DateTimeFormat.forPattern(RANGE_KEY_DATE_TEMPLATE);

    public enum AggStatsAttribute implements Attribute {
        ACCOUNT_ID ("aid", "N"),
        RANGE_KEY ("datel|dev", "S"),  // <local_date>|<external_device_id>
        DAY_OF_WEEK ("dow", "N"),

        DEVICE_DATA_LENGTH ("device_data_len", "N"),
        TRACKER_MOTION_LENGTH ("tracker_motion_len", "N"),

        AVG_DAILY_TEMP ("avg_day_temp", "N"),
        MAX_DAILY_TEMP ("max_day_temp", "N"),
        MIN_DAILY_TEMP ("min_day_temp", "N"),
        AVG_DAILY_HUMID ("avg_day_humid", "N"),
        AVG_DAILY_DUST_DENSITY ("avg_day_dust_density", "N"),

        SUM_LENGTH_MLUX_HRS_MAP ("sum_len_mlux_hrs_map", "S");

        private final String name;
        private final String type;

        AggStatsAttribute(String name, String type) {
            this.name = name;
            this.type = type;
        }

        /**
         * Useful instead of item.get(AggStatsAttribute.<AggStatsAttribute>.name) to avoid NullPointerException
         */
        private AttributeValue getAttributeFromDDBIItem(final Map<String, AttributeValue> item) {
            return item.get(this.name);
        }

        private Long getAccountIdFromDDBIItem(final Map<String, AttributeValue> item) {
            return Long.valueOf(AggStatsAttribute.ACCOUNT_ID.getAttributeFromDDBIItem(item).getN());
        }

        private DateTime getLocalDateFromDDBIItem(final Map<String, AttributeValue> item) {
            final String dateString = AggStatsDAODynamoDB.AggStatsAttribute.RANGE_KEY.getAttributeFromDDBIItem(item).getS().substring(0, DATE_TIME_STRING_TEMPLATE.length());
            return DateTime.parse(dateString, DATE_TIME_READ_FORMATTER).withZone(DateTimeZone.UTC);
        }

        private String getExtDeviceIdFromDDBIItem(final Map<String, AttributeValue> item) {
            return item.get(AggStatsDAODynamoDB.AggStatsAttribute.RANGE_KEY.name).getS().substring(DATE_TIME_STRING_TEMPLATE.length() + 1);
        }

        private Integer getIntegerFromDDBIItem(final Map<String, AttributeValue> item) {
            if (item.containsKey(this.name)) {
                return Integer.valueOf(getAttributeFromDDBIItem(item).getN());
            }
            return -999; //Default "null value" for when we change structure of dynamo table
        }

        private Map<Integer, SumLengthData> getSumLengthMapFromDDBIItem(final Map<String, AttributeValue> item) {
            //AttributeValue: "{3=[0; 0], 2=[0; 0], 1=[0; 0], 0=[0; 0], 5=[0; 0], 4=[0; 0], 22=[0; 0], 23=[0; 0]}"

            final Map<Integer, SumLengthData> sumLengthDataMap = Maps.newHashMap();

            try {
                if (item.containsKey(this.name)) {
                    final String stringToProcess = String.valueOf(getAttributeFromDDBIItem(item).getS());
                    final String[] pairs = stringToProcess.replaceAll("[\\{\\}\\s+]", "").split(",");
                    for (String pair : pairs) {
                        final String[] keyVal = pair.split("=");
                        final int key = Integer.parseInt(keyVal[0]);
                        final int sum = Integer.parseInt(keyVal[1].split(";")[0].replaceAll("[\\[\\]]", ""));
                        final int length = Integer.parseInt(keyVal[1].split(";")[1].replaceAll("[\\[\\]]", ""));
                        final SumLengthData sumLengthData = new SumLengthData(sum, length);
                        sumLengthDataMap.put(key, sumLengthData);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("exception={} action=received-malformed-attribute item={}", e.getMessage(), item.toString());
            }

            return sumLengthDataMap;
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

    public final static ImmutableSet<AggStatsDAODynamoDB.AggStatsAttribute> ALL_ATTRIBUTES = ImmutableSet.copyOf(AggStatsAttribute.values());

    public AggStatsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tablePrefix, final String version) {
        super(dynamoDBClient, tablePrefix);
        this.tableName = tablePrefix + "_" + version;
        this.version = version;
    }

    //region Override abstract methods
    @Override
    protected Logger logger() {return LOGGER; }

    @Override
    protected Integer maxQueryAttempts() {return 5; }

    @Override
    protected Integer maxBatchWriteAttempts() {return 5; }

    @Override
    protected String hashKeyName() {return AggStatsAttribute.ACCOUNT_ID.name; }

    @Override
    protected String rangeKeyName() {return AggStatsAttribute.RANGE_KEY.name; }

    @Override
    protected String hashKeyType() {return AggStatsAttribute.ACCOUNT_ID.type; }

    @Override
    protected String rangeKeyType() {return AggStatsAttribute.RANGE_KEY.type; }

    @Override
    protected String getHashKey(final AttributeValue attributeValue) {return attributeValue.getN(); }

    @Override
    protected String getRangeKey(final AttributeValue attributeValue) {return attributeValue.getS(); }

    @Override
    protected DateTime getTimestamp(final AggStats aggStats) {return aggStats.dateLocal; }

    @Override
    protected Map<String, AttributeValue> toAttributeMap(final AggStats aggStats) {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        item.put(AggStatsAttribute.ACCOUNT_ID.name, toAttributeValue(aggStats.accountId));
        item.put(AggStatsAttribute.RANGE_KEY.name, getRangeKey(aggStats.dateLocal, aggStats.externalDeviceId.toString() ));
        item.put(AggStatsAttribute.DAY_OF_WEEK.name, toAttributeValue(aggStats.dateLocal.getDayOfWeek()));

        item.put(AggStatsAttribute.DEVICE_DATA_LENGTH.name, toAttributeValue(aggStats.deviceDataLength));
        item.put(AggStatsAttribute.TRACKER_MOTION_LENGTH.name, toAttributeValue(aggStats.trackerMotionLength));

        item.put(AggStatsAttribute.AVG_DAILY_TEMP.name, toAttributeValue(aggStats.avgDailyTemp));
        item.put(AggStatsAttribute.MAX_DAILY_TEMP.name, toAttributeValue(aggStats.maxDailyTemp));
        item.put(AggStatsAttribute.MIN_DAILY_TEMP.name, toAttributeValue(aggStats.minDailyTemp));
        item.put(AggStatsAttribute.AVG_DAILY_HUMID.name, toAttributeValue(aggStats.avgDailyHumidity));
        item.put(AggStatsAttribute.AVG_DAILY_DUST_DENSITY.name, toAttributeValue(aggStats.avgDailyDustDensity));

        item.put(AggStatsAttribute.SUM_LENGTH_MLUX_HRS_MAP.name, toAttributeValue(aggStats.sumLengthMicroLuxHourMap));
        return item;
    }

    @Override
    public String getTableName(final DateTime dateTime) {
        return tableName; //don't shard for now
    }

    @Override
    public List<String> getTableNames(final DateTime start, final DateTime end) {
        final List<String> tableNames = Lists.newArrayList( tableName ); //don't shard for now
        return tableNames;
    }
    //end region

    private static AttributeValue getRangeKey(final DateTime dateTime, final String senseId) {
        return new AttributeValue(dateTime.toString(RANGE_KEY_DATE_FORMATTER) + "|" + senseId);
    }

    private static AttributeValue toAttributeValue(final Integer value) {
        return new AttributeValue().withN(String.valueOf(value));
    }

    private static AttributeValue toAttributeValue(final Long value) {
        return new AttributeValue().withN(String.valueOf(value));
    }

    private static AttributeValue toAttributeValue(final Map<Integer, SumLengthData> sumLengthDataMap) {
        //Result {S: {3=[0; 0], 2=[0; 0], 1=[0; 0], 0=[0; 0], 5=[0; 0], 4=[0; 0], 22=[0; 0], 23=[0; 0]},}

        final Map<String, String> sumLengthStringMap = Maps.newHashMap();
        for (Map.Entry<Integer, SumLengthData> entry : sumLengthDataMap.entrySet()) {
            final int sum = entry.getValue().sum;
            final int length = entry.getValue().length;
            final String sumLengthString = String.format("[%d; %d]", sum, length);
            sumLengthStringMap.put(entry.getKey().toString(), sumLengthString);
        }

        return new AttributeValue().withS(sumLengthStringMap.toString()); //TODO: sort by key?
    }

    final AggStats attributeMapToAggStats(final Map<String, AttributeValue> item) {
        return new AggStats.Builder()
                .withAccountId(AggStatsAttribute.ACCOUNT_ID.getAccountIdFromDDBIItem(item))
                .withDateLocal(AggStatsAttribute.RANGE_KEY.getLocalDateFromDDBIItem(item))
                .withExternalDeviceId(AggStatsAttribute.RANGE_KEY.getExtDeviceIdFromDDBIItem(item))

                .withDeviceDataLength(AggStatsAttribute.DEVICE_DATA_LENGTH.getIntegerFromDDBIItem(item))
                .withTrackerMotionLength(AggStatsAttribute.TRACKER_MOTION_LENGTH.getIntegerFromDDBIItem(item))

                .withAvgDailyTemp(AggStatsAttribute.AVG_DAILY_TEMP.getIntegerFromDDBIItem(item))
                .withMaxDailyTemp(AggStatsAttribute.MAX_DAILY_TEMP.getIntegerFromDDBIItem(item))
                .withMinDailyTemp(AggStatsAttribute.MIN_DAILY_TEMP.getIntegerFromDDBIItem(item))
                .withAvgDailyHumidity(AggStatsAttribute.AVG_DAILY_HUMID.getIntegerFromDDBIItem(item))
                .withAvgDailyDustDensity(AggStatsAttribute.AVG_DAILY_DUST_DENSITY.getIntegerFromDDBIItem(item))

                .withSumLenMicroLuxHourMap(AggStatsAttribute.SUM_LENGTH_MLUX_HRS_MAP.getSumLengthMapFromDDBIItem(item))
                
                .build();
    }

    /*
    Functionality
     */

    public Boolean insertSingleStat(final AggStats aggStats) {
        try {
            final List<AggStats> aggStatsList = Lists.newArrayList(aggStats);
            final int numSuccess = batchInsert(aggStatsList);
            if (numSuccess > 0) {
                LOGGER.debug("");
                return Boolean.TRUE;
            }

        } catch (AmazonServiceException ase) {
            LOGGER.error("");
        }

        return Boolean.FALSE;
    }

    //TODO: test response status when no data present, dynamo off, etc.
    public Optional<AggStats> getSingleStat(final Long accountId, final DeviceId deviceId, final DateTime dateLocal) {
        Response<ImmutableList<AggStats>> response = getBatchStatsBetweenLocalDate(accountId, deviceId, dateLocal, dateLocal);
        if (response.status != Response.Status.SUCCESS || response.exception.isPresent()) {
            LOGGER.debug("action=get-single-stat response_stats={} response_exception={} accountId={} senseId={} dateLocal={}", response.status.toString(), response.exception.toString(), accountId, deviceId.externalDeviceId, dateLocal.toString());
            return Optional.absent();
        }

        if (response.data.isEmpty()) {
            LOGGER.trace("action=get-single-stat response_data=empty accountId={} senseId={} dateLocal={}", accountId, deviceId.externalDeviceId, dateLocal.toString());
            return Optional.absent();
        }
        Optional<AggStats> aggStatsOptional = Optional.of(response.data.get(0));

        return aggStatsOptional;
    }

    public Response<ImmutableList<AggStats>> getBatchStatsBetweenLocalDate(final Long accountId, final DeviceId deviceId, final DateTime startDateLocal, final DateTime endDateLocal) {
        final String externalDeviceId = deviceId.externalDeviceId.get();

        final Expression keyConditionExp = Expressions.and(
                Expressions.equals(AggStatsDAODynamoDB.AggStatsAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(AggStatsDAODynamoDB.AggStatsAttribute.RANGE_KEY, getRangeKey(startDateLocal, externalDeviceId), getRangeKey(endDateLocal, externalDeviceId)));

        final List<AggStats> results = Lists.newArrayList();
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(startDateLocal, endDateLocal), keyConditionExp, ALL_ATTRIBUTES);
        for (final Map<String, AttributeValue> result : response.data) {
            final AggStats data = attributeMapToAggStats(result);
            if (data.externalDeviceId.equals(externalDeviceId)) {
                results.add(data);
            }
        }
        return Response.into(ImmutableList.copyOf(results), response);
    }
}

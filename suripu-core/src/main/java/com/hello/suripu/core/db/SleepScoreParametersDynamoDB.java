package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.models.SleepScoreParameters;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ksg on 5/24/16
 */
public class SleepScoreParametersDynamoDB implements SleepScoreParametersDAO{

    private final static Logger LOGGER = LoggerFactory.getLogger(SleepScoreParametersDynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public enum SleepScoreParameterAttribute implements Attribute {
        ACCOUNT_ID("account_id", "N"), // hash key
        DATE("date", "S"), // sort key
        DURATION_THRESHOLD("duration_threshold", "N"),
        MOTION_FREQUENCY_THRESHOLD("motion_frequency_threshold", "S");

        private final String name;
        private final String type;

        SleepScoreParameterAttribute(String name, String type) {
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

        public static Set<String> getAllAttributes() {
            final Set<String> attributes = Sets.newHashSet();
            for (final Attribute attribute : SleepScoreParameterAttribute.values()) {
                attributes.add(attribute.shortName());
            }
            return attributes;
        }

    }

    public SleepScoreParametersDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public CreateTableResult createTable(final Long readCapacity, final Long writeCapacity) {
        return Util.createTable(dynamoDBClient, tableName,
                SleepScoreParameterAttribute.ACCOUNT_ID, SleepScoreParameterAttribute.DATE,
                readCapacity, writeCapacity);
    }

    @Override
    public SleepScoreParameters getSleepScoreParametersByDate(final Long accountId, final DateTime dateTimeUTC) {

        final Map<String, Condition> queryCondition = getQueryConditions(accountId, dateTimeUTC);
        final Set<String> targetAttributeSet = SleepScoreParameterAttribute.getAllAttributes();

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryCondition)
                .withAttributesToGet(targetAttributeSet)
                .withLimit(1)
                .withScanIndexForward(false);

        final QueryResult queryResult;
        try {
            queryResult = this.dynamoDBClient.query(queryRequest);
        } catch (AmazonServiceException ase) {
            LOGGER.error("error=query-sleep-score-parameters-fail account_id={} date={}", accountId, dateTimeUTC);
            return new SleepScoreParameters(accountId, dateTimeUTC); // return default values
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();
        if (items == null || items.isEmpty()) {
            return new SleepScoreParameters(accountId, dateTimeUTC); // return default values
        }

        // get the last updated parameters
        final Map<String, AttributeValue> item = queryResult.getItems().get(0);
        return toSleepScoreParameters(item);
    }


    @Override
    public Boolean upsertSleepScoreParameters(final Long accountId, final SleepScoreParameters parameter) {

        final Map<String, AttributeValueUpdate> updateItem = Maps.newHashMap();
        updateItem.put(SleepScoreParameterAttribute.DURATION_THRESHOLD.shortName(), Util.putAction(parameter.durationThreshold));
        updateItem.put(SleepScoreParameterAttribute.MOTION_FREQUENCY_THRESHOLD.shortName(), Util.putAction(parameter.motionFrequencyThreshold.toString()));

        final Map<String, AttributeValue> key = getKey(accountId, parameter.dateTime);
        final UpdateItemResult result = dynamoDBClient.updateItem(tableName, key, updateItem, "ALL_NEW");

        if (result.getAttributes() == null) {
            return false;
        }

        if (!result.getAttributes().isEmpty()) {
            final AttributeValue durationResultValue = result.getAttributes().get(SleepScoreParameterAttribute.DURATION_THRESHOLD.shortName());
            final AttributeValue motionFrequencyResultValue = result.getAttributes().get(SleepScoreParameterAttribute.MOTION_FREQUENCY_THRESHOLD.shortName());
            final AttributeValue durationThresholdValue = new AttributeValue().withN(String.valueOf(parameter.durationThreshold));
            final AttributeValue motionFrequencyThresholdValue = new AttributeValue().withS(String.valueOf(parameter.motionFrequencyThreshold));
            if (durationResultValue.equals(durationThresholdValue) & motionFrequencyResultValue.equals(motionFrequencyThresholdValue)) {
                return true;
            }
        }

        return false;
    }


    private SleepScoreParameters toSleepScoreParameters(final Map<String, AttributeValue> item) {

        final String accountIdString = item.get(SleepScoreParameterAttribute.ACCOUNT_ID.shortName()).getN();
        final String parameterDate = item.get(SleepScoreParameterAttribute.DATE.shortName()).getS();

        // check for attribute presence
        final Integer durationThreshold;
        if (item.containsKey(SleepScoreParameterAttribute.DURATION_THRESHOLD.shortName())) {
            durationThreshold = Integer.valueOf(item.get(SleepScoreParameterAttribute.DURATION_THRESHOLD.shortName()).getN());
        } else {
            durationThreshold = SleepScoreParameters.MISSING_THRESHOLD;
        }
        final Float motionFrequencyThreshold;
        if (item.containsKey(SleepScoreParameterAttribute.MOTION_FREQUENCY_THRESHOLD.shortName())) {
            motionFrequencyThreshold = Float.valueOf(item.get(SleepScoreParameterAttribute.MOTION_FREQUENCY_THRESHOLD.shortName()).getS());
        } else {
            motionFrequencyThreshold =(float) SleepScoreParameters.MISSING_THRESHOLD;
        }

        return new SleepScoreParameters(
                Long.valueOf(accountIdString),
                DateTimeUtil.ymdStringToDateTime(parameterDate),
                durationThreshold,
                motionFrequencyThreshold
        );
    }


    private Map<String, AttributeValue> getKey(final Long accountId, final DateTime dateTime) {
        final Map<String, AttributeValue> key = Maps.newHashMap();
        key.put(SleepScoreParameterAttribute.ACCOUNT_ID.shortName(), new AttributeValue().withN(String.valueOf(accountId)));

        final String date = DateTimeUtil.dateToYmdString(dateTime);
        key.put(SleepScoreParameterAttribute.DATE.shortName(), new AttributeValue().withS(date));

        return ImmutableMap.copyOf(key);
    }


    private Map<String, Condition> getQueryConditions(final Long accountId, final DateTime dateTime) {
        final Map<String, Condition> queryCondition = Maps.newHashMap();

        final Condition accountCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(accountId.toString()));
        queryCondition.put(SleepScoreParameterAttribute.ACCOUNT_ID.shortName(), accountCondition);

        final String dateString = DateTimeUtil.dateToYmdString(dateTime);
        final Condition dateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LE)
                .withAttributeValueList(new AttributeValue().withS(dateString));
        queryCondition.put(SleepScoreParameterAttribute.DATE.shortName(), dateCondition);

        return ImmutableMap.copyOf(queryCondition);
    }


}

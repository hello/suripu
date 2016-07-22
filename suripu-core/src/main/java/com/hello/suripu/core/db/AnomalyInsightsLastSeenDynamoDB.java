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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.models.Insights.AnomalyInsightsLastSeen;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by jarredheinrich on 7/21/16.
 */
public class AnomalyInsightsLastSeenDynamoDB implements AnomalyInsightsLastSeenDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(AnomalyInsightsLastSeenDynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public enum AnomalyInsightsLastSeenAttribute implements Attribute {
        ACCOUNT_ID("account_id", "N"), // hash-key
        CATEGORY ("category", "N"), // sort key
        UPDATED("updated_utc", "S");

        private final String name;
        private final String type;

        AnomalyInsightsLastSeenAttribute(final String name, final String type) {
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
            for (final Attribute attribute : AnomalyInsightsLastSeenAttribute.values()) {
                attributes.add(attribute.shortName());
            }
            return attributes;
        }
    }


    public AnomalyInsightsLastSeenDynamoDB (final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public CreateTableResult createTable(final Long readCapacityUnits, final Long writeCapacityUnits) {

        return Util.createTable(dynamoDBClient, tableName,
                AnomalyInsightsLastSeenAttribute.ACCOUNT_ID, AnomalyInsightsLastSeenAttribute.CATEGORY,
                readCapacityUnits, writeCapacityUnits);
    }

    public ImmutableList<AnomalyInsightsLastSeen> getAnomalyInsightsByAccountId(final Long accountId) {

        final Map<String, Condition> queryCondition = getQueryConditions(accountId);
        final Set<String> targetAttributeSet = AnomalyInsightsLastSeenAttribute.getAllAttributes();

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryCondition)
                .withAttributesToGet(targetAttributeSet);

        final QueryResult queryResult;
        try{
            queryResult = this.dynamoDBClient.query(queryRequest);
        }catch (AmazonServiceException ase) {
            LOGGER.error("error=query-anomaly-insights-last-seen-fail account_id={}", accountId);
            return ImmutableList.of(new AnomalyInsightsLastSeen(accountId));
        }
        final List<Map<String, AttributeValue>> items = queryResult.getItems();
        if (items == null || items.isEmpty()) {
            return ImmutableList.of(new AnomalyInsightsLastSeen(accountId));
        }

        return toAnomalyInsightsLastSeen(items);
    }

    public AnomalyInsightsLastSeen getAnomalyInsightsByAccountIdAndCategory(final Long accountId, final InsightCard.Category category) {

        final Map<String, Condition> queryCondition = getQueryConditionsWithCategory(accountId, category);
        final Set<String> targetAttributeSet = AnomalyInsightsLastSeenAttribute.getAllAttributes();

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryCondition)
                .withAttributesToGet(targetAttributeSet);

        final QueryResult queryResult;
        try{
            queryResult = this.dynamoDBClient.query(queryRequest);
        }catch (AmazonServiceException ase) {
            LOGGER.error("error=query-anomaly-insights-by-category-fail account_id={}", accountId);
            return new AnomalyInsightsLastSeen(accountId);
        }
        final List<Map<String, AttributeValue>> item = queryResult.getItems();
        if (item == null || item.isEmpty()) {
            return new AnomalyInsightsLastSeen(accountId);
        }
        ImmutableList<AnomalyInsightsLastSeen> anomalyInsightsByCategory = toAnomalyInsightsLastSeen(item);
        if (anomalyInsightsByCategory.size() > 1){
            LOGGER.error("error=query-anomaly-insights-by-category-failed-too-many account_id={} number={}", accountId, anomalyInsightsByCategory.size());
        }
        return anomalyInsightsByCategory.get(0);
    }


    private ImmutableList<AnomalyInsightsLastSeen> toAnomalyInsightsLastSeen(final List<Map<String, AttributeValue>> items) {
        final List<AnomalyInsightsLastSeen> anomalyInsightsLastSeenList = new ArrayList<>();
        for( final Map<String, AttributeValue> item: items) {
            Long accountId = Long.valueOf(item.get(AnomalyInsightsLastSeenAttribute.ACCOUNT_ID.shortName()).getN());
            DateTime lastSeenDate = DateTimeUtil.ymdStringToDateTime(item.get(AnomalyInsightsLastSeenAttribute.UPDATED.shortName()).getS());
            InsightCard.Category category = InsightCard.Category.fromInteger(Integer.valueOf(item.get(AnomalyInsightsLastSeenAttribute.CATEGORY.shortName()).getN()));
            anomalyInsightsLastSeenList.add(new AnomalyInsightsLastSeen(accountId, Optional.of(category), Optional.of(lastSeenDate)));

        }

        return ImmutableList.copyOf(anomalyInsightsLastSeenList);
    }

    public Boolean upsertAnomalyInsightsLastSeen(AnomalyInsightsLastSeen anomalyInsight) {
        final Map<String, AttributeValueUpdate> updateItem = Maps.newHashMap();
        updateItem.put(AnomalyInsightsLastSeenAttribute.UPDATED.shortName(), Util.putAction(DateTimeUtil.dateToYmdString(anomalyInsight.updatedUTC.get())));
        final Map<String, AttributeValue> key = Maps.newHashMap();

        key.put(AnomalyInsightsLastSeenAttribute.ACCOUNT_ID.shortName(), new AttributeValue().withN(String.valueOf(anomalyInsight.accountId)));
        key.put(AnomalyInsightsLastSeenAttribute.CATEGORY.shortName(), new AttributeValue().withN(String.valueOf(anomalyInsight.seenCategory.get().getValue())));

        final UpdateItemResult result = dynamoDBClient.updateItem(tableName, key, updateItem, "ALL_NEW");

        if (result.getAttributes() == null) {
            LOGGER.error("error=upsert-anomaly-insights-last-seen-fail account_id={}", anomalyInsight.accountId);
            return false;
        }

        if (! result.getAttributes().isEmpty()) {
            final AttributeValue resultCategory = result.getAttributes().get(AnomalyInsightsLastSeenAttribute.CATEGORY.shortName());
            final AttributeValue anomalyCategory = new AttributeValue().withN(String.valueOf(anomalyInsight.seenCategory.get().getValue()));
            final AttributeValue resultDate = result.getAttributes().get(AnomalyInsightsLastSeenAttribute.UPDATED.shortName());
            final AttributeValue anomalyDate = new AttributeValue().withS(DateTimeUtil.dateToYmdString(anomalyInsight.updatedUTC.get()));
            if (resultCategory.equals(anomalyCategory) && resultDate.equals(anomalyDate)){
                return true;
            }
        }
        LOGGER.error("error=upsert-anomaly-insights-last-seen-fail account_id={}", anomalyInsight.accountId);
        return false;
    }


    private Map<String, Condition> getQueryConditions(final Long accountId) {
        final Map<String, Condition> queryCondition = Maps.newHashMap();

        final Condition accountCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(accountId.toString()));
        queryCondition.put(AnomalyInsightsLastSeenAttribute.ACCOUNT_ID.shortName(), accountCondition);

        return ImmutableMap.copyOf(queryCondition);
    }

    private Map<String, Condition> getQueryConditionsWithCategory(final Long accountId, final InsightCard.Category category) {
        final Map<String, Condition> queryCondition = Maps.newHashMap();

        final Condition accountCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(accountId.toString()));
        queryCondition.put(AnomalyInsightsLastSeenAttribute.ACCOUNT_ID.shortName(), accountCondition);

        final Condition categoryCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(category.getValue())));
        queryCondition.put(AnomalyInsightsLastSeenAttribute.CATEGORY.shortName(), categoryCondition);

        return ImmutableMap.copyOf(queryCondition);
    }

}

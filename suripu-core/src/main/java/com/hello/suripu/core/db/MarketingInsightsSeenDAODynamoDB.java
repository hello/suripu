package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.MarketingInsightsSeen;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ksg on 3/25/16
 */

public class MarketingInsightsSeenDAODynamoDB {
    private final static Logger LOGGER = LoggerFactory.getLogger(MarketingInsightsSeenDAODynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;
    private final ImmutableSet<String> requiredAttributes;


    public enum MarketingInsightsSeenAttribute implements Attribute {
        ACCOUNT_ID("account_id", "N"), // hash-key
        CATEGORIES("categories", "NS"),
        UPDATED("updated_utc", "S");

        private final String name;
        private final String type;

        MarketingInsightsSeenAttribute(final String name, final String type) {
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
    }


    private Map<String, AttributeValue> getKey(final Long accountId) {
        return ImmutableMap.of(MarketingInsightsSeenAttribute.ACCOUNT_ID.shortName(), new AttributeValue().withN(String.valueOf(accountId)));
    }


    public MarketingInsightsSeenDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;

        final Set<String> names = Sets.newHashSet();
        for (final MarketingInsightsSeenAttribute attribute : MarketingInsightsSeenAttribute.values()) {
            names.add(attribute.shortName());
        }
        this.requiredAttributes = ImmutableSet.copyOf(names);
    }


   public CreateTableResult createTable(final Long readCapacityUnits, final Long writeCapacityUnits) {
       return Util.createTable(dynamoDBClient, tableName, MarketingInsightsSeenAttribute.ACCOUNT_ID, readCapacityUnits, writeCapacityUnits);
   }


    public Optional<MarketingInsightsSeen> getSeenCategories(final Long accountId) {

        final Map<String, AttributeValue> key = getKey(accountId);
        final Response<Optional<Map<String, AttributeValue>>> response = Util.getWithBackoff(dynamoDBClient, tableName, key);

        if (response.data.isPresent()) {
            return fromDynamoDBItem(response.data.get());
        }

        return Optional.absent();
    }


    public boolean updateSeenCategories(final Long accountId, final InsightCard.Category category) {
        final Map<String, AttributeValueUpdate> updates = Maps.newHashMap();

        updates.put(MarketingInsightsSeenAttribute.CATEGORIES.shortName(), Util.addToSetAction(category.getValue()));

        final String now = DateTime.now(DateTimeZone.UTC).toString(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT);
        updates.put(MarketingInsightsSeenAttribute.UPDATED.shortName(), Util.putAction(now));

        // update item, and return the all-new version
        final Map<String, AttributeValue> key = getKey(accountId);
        final UpdateItemResult result = dynamoDBClient.updateItem(tableName, key, updates, "ALL_NEW");

        if (result.getAttributes() == null) {
            return false;
        }

        if (!result.getAttributes().isEmpty()) {
            final List<String> categories = result.getAttributes().get(MarketingInsightsSeenAttribute.CATEGORIES.shortName()).getNS();
            final String updatedCategory = String.valueOf(category.getValue());

            if (categories.contains(updatedCategory)) {
                return true;
            }
        }

        // AND THEN I TOLD HER TO STOP BEING A STUPID COW, CRASHING ALL THE TIME
        return false;
    }

    private Optional<MarketingInsightsSeen> fromDynamoDBItem(final Map<String, AttributeValue> item) {
        if (!item.keySet().containsAll(this.requiredAttributes)) {
            LOGGER.warn("key=marketing-insights-ddb warning=missing-attributes values={}", item.keySet());
            return Optional.absent();
        }

        final List<String> categories = item.get(MarketingInsightsSeenAttribute.CATEGORIES.shortName()).getNS();
        final Set<InsightCard.Category> insightCategories = Sets.newHashSet();
        for (final String category: categories) {
            insightCategories.add(InsightCard.Category.fromInteger(Integer.valueOf(category)));
        }

        final String updated = item.get(MarketingInsightsSeenAttribute.UPDATED.shortName()).getS();
        return Optional.of(new MarketingInsightsSeen(insightCategories, DateTimeUtil.datetimeStringToDateTime(updated)));
    }
}
package com.hello.suripu.core.insights;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Created by jarredheinrich on 7/21/16.
 */
public class InsightsLastSeenDynamoDB implements InsightsLastSeenDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(InsightsLastSeenDynamoDB.class);

    private final Table table;

    public static enum AttributeName implements Attribute {
        ACCOUNT_ID("account_id", "N"); // hash-key

        private final String name;
        private final String type;

        AttributeName(final String name, final String type) {
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
            for (final Attribute attribute : AttributeName.values()) {
                attributes.add(attribute.shortName());
            }
            return attributes;
        }
    }


    private InsightsLastSeenDynamoDB(final Table table) {
        this.table = table;

    }

    public static InsightsLastSeenDynamoDB create(final AmazonDynamoDB client, final String tableName){
        final DynamoDB dynamoDB = new DynamoDB(client);
        final Table table = dynamoDB.getTable(tableName);
        return new InsightsLastSeenDynamoDB(table);
    }

    public static TableDescription createTable(final AmazonDynamoDB client, final String tableName) {

        final CreateTableResult result =  Util.createTable(client, tableName, AttributeName.ACCOUNT_ID, 1L, 1L);

        return result.getTableDescription();
    }


    public ImmutableList<InsightsLastSeen> getAll(final Long accountId) {
        final PrimaryKey key = new PrimaryKey(AttributeName.ACCOUNT_ID.shortName(), accountId);
        final Item item = table.getItem(key);

        final ImmutableList<InsightsLastSeen> insightsLastSeens = fromItem(accountId, item);

        return insightsLastSeens;
    }

    public Optional<InsightsLastSeen> getFor(final Long accountId, final InsightCard.Category category) {

        final PrimaryKey key = new PrimaryKey(AttributeName.ACCOUNT_ID.shortName(), accountId);
        final GetItemSpec spec = new GetItemSpec().withPrimaryKey(key).withAttributesToGet(category.name());
        final Item item = table.getItem(spec);
        if(item.hasAttribute(category.name())){
            return Optional.of(new InsightsLastSeen(accountId, category, item.getLong(category.name())));
        }

        return Optional.absent();
    }


    private ImmutableList<InsightsLastSeen> fromItem(final Long accountId, final Item item) {
        final List<InsightsLastSeen> insightsLastSeenList = new ArrayList<>();

        for (final InsightCard.Category  category : InsightCard.Category.values()){
            if(item.hasAttribute(category.name())){
               final InsightsLastSeen insightLastSeen = new InsightsLastSeen(accountId, category, item.getLong(category.name()));
                insightsLastSeenList.add(insightLastSeen);
            }

        }
        return ImmutableList.copyOf(insightsLastSeenList);
    }

    public Boolean markLastSeen(InsightsLastSeen insightLastSeen) {
        final PrimaryKey key = new PrimaryKey(AttributeName.ACCOUNT_ID.shortName(), insightLastSeen.accountId);

        final AttributeUpdate attribute = new AttributeUpdate(insightLastSeen.seenCategory.name());
        attribute.put(insightLastSeen.updatedUTC.getMillis());
        final UpdateItemSpec itemSpec = new UpdateItemSpec().withPrimaryKey(key).addAttributeUpdate(attribute);

        final UpdateItemOutcome updatedItem = table.updateItem(itemSpec);

        final Item item = updatedItem.getItem();

        if (!item.hasAttribute(insightLastSeen.seenCategory.name())) {
            LOGGER.error("error=mark-last-seen-insights-fail-insight-missing account_id={}", insightLastSeen.accountId);
            return false;
        }else if (item.getLong(insightLastSeen.seenCategory.name())!= insightLastSeen.updatedUTC.getMillis()) {
            LOGGER.error("error=mark-last-seen-insights-fail-wrong-timestamp account_id={}", insightLastSeen.accountId);
            return false;
        }

        return true;
    }


}

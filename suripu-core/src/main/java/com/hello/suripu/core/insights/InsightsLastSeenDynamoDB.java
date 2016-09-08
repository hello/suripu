package com.hello.suripu.core.insights;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by jarredheinrich on 7/21/16.
 */
public class InsightsLastSeenDynamoDB implements InsightsLastSeenDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(InsightsLastSeenDynamoDB.class);

    private final Table table;

    public static enum AttributeName implements Attribute {
        ACCOUNT_ID("account_id", "N"), // hash-key
        LAST_UPDATED_UTC("last_updated_utc", "N");
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
        if (item == null) {
            LOGGER.debug("debug=get-all-last-seen-insights-fail-insight-missing account_id={}", accountId);
            final List<InsightsLastSeen> insightsLastSeen = new ArrayList<>();
            return ImmutableList.copyOf(insightsLastSeen);
        }
        final ImmutableList<InsightsLastSeen> insightsLastSeen = fromItem(accountId, item);
        return insightsLastSeen;


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

    public Boolean markLastSeen(final InsightsLastSeen insightLastSeen) {
        final PrimaryKey key = new PrimaryKey(AttributeName.ACCOUNT_ID.shortName(), insightLastSeen.accountId);

        final AttributeUpdate attribute = new AttributeUpdate(insightLastSeen.seenCategory.name());
        attribute.put(insightLastSeen.updatedUTC.getMillis());

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey("account_id", insightLastSeen.accountId)
                .withReturnValues(ReturnValue.ALL_NEW)
                .withUpdateExpression("set #category = :val1 ,  #account_last_updated = :val1")
                .withNameMap(new NameMap()
                        .with("#category", insightLastSeen.seenCategory.name())
                        .with("#account_last_updated", AttributeName.LAST_UPDATED_UTC.shortName()))
                .withValueMap(new ValueMap()
                        .withNumber(":val1", insightLastSeen.updatedUTC.getMillis()));


        final UpdateItemOutcome updatedItem = table.updateItem(updateItemSpec);
        final Item item = updatedItem.getItem();

        //check for category
        if (!item.hasAttribute(insightLastSeen.seenCategory.name())) {
            LOGGER.error("error=mark-last-seen-insights-fail-insight-missing account_id={}", insightLastSeen.accountId);
            return false;
        }
        final long updatedUTC = item.getLong(insightLastSeen.seenCategory.name());

        //check that lastSeen time is correct
        if (updatedUTC != insightLastSeen.updatedUTC.getMillis()) {
            LOGGER.error("error=mark-last-seen-insights-fail-wrong-timestamp account_id={}", insightLastSeen.accountId);
            return false;
        }
        //check that last updated time for account is correct
        if (updatedUTC!= insightLastSeen.updatedUTC.getMillis()) {
            LOGGER.error("error=mark-last-seen-insights-fail-wrong-last-updated-timestamp account_id={}", insightLastSeen.accountId);
            return false;
        }

        return true;
    }

}

package com.hello.suripu.core.analytics;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;

public class AnalyticsTrackingDynamoDB implements AnalyticsTrackingDAO {

    public enum InAppNotificationAttribute implements Attribute {
        ACCOUNT_ID ("account_id", "N"),
        PILL_LOW_BATTERY("pill_low_battery", "S");

        private final String name;
        private final String type;

        InAppNotificationAttribute (final String name, final String type) {
            this.name = name;
            this.type = type;
        }

        private Long getLong(final Map<String, AttributeValue> item) {
            if (item.containsKey(this.name)) {
                return Long.parseLong(item.get(this.name).getN());
            }
            return 0L;
        }

        private Integer getInteger(final Map<String, AttributeValue> item) {
            if (item.containsKey(this.name)) {
                return Integer.parseInt(item.get(this.name).getN());
            }
            return 0;
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

    private final Table table;

    private AnalyticsTrackingDynamoDB(final Table table) {
        this.table = table;
    }

    public static AnalyticsTrackingDynamoDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new AnalyticsTrackingDynamoDB(table);
    }


    @Override
    public boolean putIfAbsent(TrackingEvent event, Long accountId) {
        switch (event) {
            case PILL_LOW_BATTERY:
                return putIfAbsent(InAppNotificationAttribute.PILL_LOW_BATTERY, accountId, DateTime.now(DateTimeZone.UTC));
        }

        return false;
    }

    @Override
    public boolean putIfAbsent(final TrackingEvent event, final Long accountId, final DateTime dateTime) {
        switch (event) {
            case PILL_LOW_BATTERY:
                return putIfAbsent(InAppNotificationAttribute.PILL_LOW_BATTERY, accountId, dateTime);
        }

        return false;
    }

    private boolean putIfAbsent(InAppNotificationAttribute attribute, Long accountId, DateTime dateTime) {
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey("account_id", accountId)
                .withReturnValues(ReturnValue.UPDATED_OLD)
                .withUpdateExpression("set #n = if_not_exists(#n, :val1)")
                .withNameMap(new NameMap()
                        .with("#n", attribute.shortName()))
                .withValueMap(new ValueMap()
                        .withString(":val1", DateTimeUtil.datetimeToString(DateTime.now(DateTimeZone.UTC))));

        final UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
        return outcome.getItem() == null;
    }

    @Override
    public Optional<TrackingEvent> get(Long accountId) {
        return Optional.absent();
    }


    public static CreateTableResult createTable(final AmazonDynamoDBClient dynamoDBClient, final String tableName){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(InAppNotificationAttribute.ACCOUNT_ID.shortName()).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(InAppNotificationAttribute.ACCOUNT_ID.shortName()).withAttributeType(ScalarAttributeType.N)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}

package com.hello.suripu.core.sense.voice;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.SenseStateDynamoDB;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.models.SenseStateAtTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoiceMetadataDAODynamoDB implements VoiceMetadataDAO {

    private static Logger LOGGER = LoggerFactory.getLogger(VoiceMetadataDAODynamoDB.class);

    public enum MetadataAttribute implements Attribute {
        SENSE_ID ("sense_id", "S"),
        PRIMARY_ACCOUNT("primary_account", "N");

        private final String name;
        private final String type;

        MetadataAttribute (final String name, final String type) {
            this.name = name;
            this.type = type;
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
    private final SenseStateDynamoDB senseStateDynamoDB;

    private VoiceMetadataDAODynamoDB(final Table table, final SenseStateDynamoDB senseStateDynamoDB) {
        this.table = table;
        this.senseStateDynamoDB = senseStateDynamoDB;
    }

    public static VoiceMetadataDAODynamoDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName, final SenseStateDynamoDB senseStateDynamoDB) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new VoiceMetadataDAODynamoDB(table, senseStateDynamoDB);

    }

    @Override
    public void updatePrimaryAccount(final String senseId, final long accountId) {
        final AttributeUpdate update = new AttributeUpdate(MetadataAttribute.PRIMARY_ACCOUNT.shortName());
        update.put(String.valueOf(accountId));

        final PrimaryKey primaryKey = new PrimaryKey(MetadataAttribute.SENSE_ID.shortName(), senseId);
        final UpdateItemOutcome outcome = table.updateItem(primaryKey, update);
    }

    @Override
    public Optional<Long> getPrimaryAccount(String senseId) {
        final Item item = table.getItem(MetadataAttribute.SENSE_ID.shortName(), senseId);
        if(item == null) {
            return Optional.absent();
        }
        final Long primaryAccountId = item.getLong(MetadataAttribute.PRIMARY_ACCOUNT.shortName());
        return Optional.fromNullable(primaryAccountId);
    }

    @Override
    public VoiceMetadata get(final String senseId, final Long currentAccount, final Long primaryAccount) {

        final Optional<SenseStateAtTime> senseStateAtTime = senseStateDynamoDB.getState(senseId);
        if(senseStateAtTime.isPresent()) {
            return VoiceMetadata.create(senseId, currentAccount, senseStateAtTime.get().muted, senseStateAtTime.get().systemVolume, primaryAccount);
        }

        return VoiceMetadata.create(senseId, currentAccount, primaryAccount);
    }


    public static void createTable(AmazonDynamoDB amazonDynamoDB, String tableName) throws InterruptedException {
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(MetadataAttribute.SENSE_ID.shortName()).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(MetadataAttribute.SENSE_ID.shortName()).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        Table table = new DynamoDB(amazonDynamoDB).createTable(request);
        table.waitForActive();
    }
}

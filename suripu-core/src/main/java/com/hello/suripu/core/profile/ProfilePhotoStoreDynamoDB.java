package com.hello.suripu.core.profile;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.MultiDensityImage;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfilePhotoStoreDynamoDB implements ProfilePhotoStore {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProfilePhotoStoreDynamoDB.class);

    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT);

    private enum AttributeName {
        ACCOUNT_ID("account_id"),
        CREATED_AT("created_at"),
        DENSITY_NORMAL("density_normal"),
        DENSITY_HIGH("density_high"),
        DENSITY_EXTRA_HIGH("density_extra_high");

        private String value;
        AttributeName(String value) {
            this.value = value;
        }
    }

    private final Table table;


    private ProfilePhotoStoreDynamoDB(final Table table) {
        this.table = table;
    }

    public static ProfilePhotoStoreDynamoDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new ProfilePhotoStoreDynamoDB(table);
    }

    @Override
    public Optional<ImmutableProfilePhoto> get(final Long accountId) {
        final Item item = table.getItem(AttributeName.ACCOUNT_ID.value, accountId);
        if(item == null) {
            return Optional.absent();
        }

        final DateTime createdAt = DateTime.parse(
                item.getString(AttributeName.CREATED_AT.value),
                dateTimeFormatter
        );

        final MultiDensityImage multiDensityImage = new MultiDensityImage(
                Optional.fromNullable(item.getString(AttributeName.DENSITY_NORMAL.value)),
                Optional.fromNullable(item.getString(AttributeName.DENSITY_HIGH.value)),
                Optional.fromNullable(item.getString(AttributeName.DENSITY_EXTRA_HIGH.value))
        );

        final ImmutableProfilePhoto profilePhoto = ImmutableProfilePhoto
                .builder()
                .accountId(accountId)
                .createdAt(createdAt)
                .photo(multiDensityImage)
                .build();

        return Optional.of(profilePhoto);
    }


    @Override
    public boolean put(final ProfilePhoto profilePhoto) {


        final Item item = new Item()
                .withLong(AttributeName.ACCOUNT_ID.value, profilePhoto.accountId())
                .withString(AttributeName.CREATED_AT.value, profilePhoto.createdAt().toString(dateTimeFormatter));

        if(profilePhoto.photo().phoneDensityNormal().isPresent()) {
            item.withString(AttributeName.DENSITY_NORMAL.value, profilePhoto.photo().phoneDensityNormal().get());
        }
        if(profilePhoto.photo().phoneDensityHigh().isPresent()) {
            item.withString(AttributeName.DENSITY_HIGH.value, profilePhoto.photo().phoneDensityNormal().get());
        }
        if(profilePhoto.photo().phoneDensityExtraHigh().isPresent()) {
            item.withString(AttributeName.DENSITY_EXTRA_HIGH.value, profilePhoto.photo().phoneDensityNormal().get());
        }

        table.putItem(item);
        // TODO: be smart about return value. Maybe catch exception?
        return true;
    }

    @Override
    public void delete(Long accountId) {
        LOGGER.info("action=delete-profile-photo account_id={}", accountId);
        table.deleteItem(new PrimaryKey(AttributeName.ACCOUNT_ID.value, accountId));
    }

    public static void createTable(final AmazonDynamoDB amazonDynamoDB, final String tableName) throws InterruptedException {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.createTable(
                tableName,
                Lists.newArrayList(
                    new KeySchemaElement().withAttributeName(AttributeName.ACCOUNT_ID.value).withKeyType(KeyType.HASH)
                ),
                Lists.newArrayList(
                        new AttributeDefinition().withAttributeName(AttributeName.ACCOUNT_ID.value).withAttributeType(ScalarAttributeType.N)
                ),
                new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
        );

        table.waitForActive();
    }
}

package com.hello.suripu.core.db.sleep_sounds;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.SleepSoundSetting;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by ksg on 02/28/2017
*/
public class SleepSoundSettingsDynamoDB {
    private final static Logger LOGGER = LoggerFactory.getLogger(SleepSoundSettingsDynamoDB.class);

    private final DynamoDB dynamoDB;
    private final Table table;


    public enum SleepSoundSettingAttributes implements Attribute {
        SENSE_ID("sense_id", "S"),  // Hash key
        ACCOUNT_ID("account_id", "N"), // Sort key
        DATETIME("datetime", "S"),
        DURATION_ID("duration_id", "N"),
        DURATION_NAME("duration_name", "S"),
        DURATION_SECONDS("duration_secs", "N"),
        SOUND_ID("sound_id", "N"),
        SOUND_PREVIEW_URL("sound_preview_url", "S"),
        SOUND_NAME("sound_name", "S"),
        SOUND_PATH("sound_path", "S"),
        SOUND_URL("sound_url", "S"),
        VOLUME_FACTOR("volume_factor", "N");

        private final String name;
        private final String type;

        SleepSoundSettingAttributes(String name, String type) {
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

    private SleepSoundSettingsDynamoDB(final DynamoDB dynamoDB, final Table table) {
        this.dynamoDB = dynamoDB;
        this.table = table;
    }

    public static SleepSoundSettingsDynamoDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SleepSoundSettingsDynamoDB(dynamoDB, table);
    }

    public static void createTable(AmazonDynamoDB amazonDynamoDB, String tableName) throws InterruptedException {
        final CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(
                        new KeySchemaElement().withAttributeName(SleepSoundSettingAttributes.SENSE_ID.shortName()).withKeyType(KeyType.HASH),
                        new KeySchemaElement().withAttributeName(SleepSoundSettingAttributes.ACCOUNT_ID.shortName()).withKeyType(KeyType.RANGE))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(SleepSoundSettingAttributes.SENSE_ID.shortName()).withAttributeType(ScalarAttributeType.S),
                        new AttributeDefinition().withAttributeName(SleepSoundSettingAttributes.ACCOUNT_ID.shortName()).withAttributeType(ScalarAttributeType.N))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

        Table table = new DynamoDB(amazonDynamoDB).createTable(request);
        table.waitForActive();
    }


    public Optional<SleepSoundSetting> get(final String senseId, final Long accountId) {
        final Item item = table.getItem(SleepSoundSettingAttributes.SENSE_ID.shortName(), senseId, SleepSoundSettingAttributes.ACCOUNT_ID.shortName(), accountId);
        if (item != null) {
            return Optional.of(fromItem(item));
        }

        return Optional.absent();
    }

    /**
     * WARNING: Last write wins
     */
    public void update(final SleepSoundSetting setting) {
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(SleepSoundSettingAttributes.SENSE_ID.shortName(), setting.senseId,
                        SleepSoundSettingAttributes.ACCOUNT_ID.shortName(), setting.accountId)
                .withAttributeUpdate(toUpdates(setting))
                .withReturnValues(ReturnValue.ALL_NEW);

        final UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
        if (outcome.getItem() != null && outcome.getItem().numberOfAttributes() > 0) {
            final String loggableAttributes = outcome.getItem().attributes().toString();
            LOGGER.info("info=changed-sound-setting-attributes sense_id={} account_id={} attributes={}", setting.senseId, setting.accountId, loggableAttributes);
        } else {
            LOGGER.error("error=fail-to-update-sound-setting sense_id={} account_id={} setting={}",  setting.senseId, setting.accountId, setting.toString());
        }
    }

    private SleepSoundSetting fromItem(final Item item) {
        final Duration duration = Duration.create(
                item.getLong(SleepSoundSettingAttributes.DURATION_ID.shortName()),
                item.getString(SleepSoundSettingAttributes.DURATION_NAME.shortName()),
                item.getInt(SleepSoundSettingAttributes.DURATION_SECONDS.shortName()));

        final Sound sound = Sound.create(
                item.getLong(SleepSoundSettingAttributes.SOUND_ID.shortName()),
                item.getString(SleepSoundSettingAttributes.SOUND_PREVIEW_URL.shortName()),
                item.getString(SleepSoundSettingAttributes.SOUND_NAME.shortName()),
                item.getString(SleepSoundSettingAttributes.SOUND_PATH.shortName()),
                item.getString(SleepSoundSettingAttributes.SOUND_URL.shortName()));

        final Integer volumeScalingFactor = item.getInt(SleepSoundSettingAttributes.VOLUME_FACTOR.shortName());

        final String senseId = item.getString(SleepSoundSettingAttributes.SENSE_ID.shortName());
        final Long accountId = item.getLong(SleepSoundSettingAttributes.ACCOUNT_ID.shortName());

        final DateTime dateTime = DateTime.parse(
                item.getString(SleepSoundSettingAttributes.DATETIME.shortName())
        ).withZone(DateTimeZone.UTC);

        return SleepSoundSetting.create(senseId, accountId, dateTime, sound, duration, volumeScalingFactor);
    }

    private List<AttributeUpdate> toUpdates(final SleepSoundSetting setting) {
        final List<AttributeUpdate> updates = Lists.newArrayList();
        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.DURATION_ID.shortName()).put(setting.duration.id));
        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.DURATION_NAME.shortName()).put(setting.duration.name));
        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.DURATION_SECONDS.shortName()).put(setting.duration.durationSeconds.get()));

        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.SOUND_ID.shortName()).put(setting.sound.id));
        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.SOUND_PREVIEW_URL.shortName()).put(setting.sound.previewUrl));
        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.SOUND_NAME.shortName()).put(setting.sound.name));
        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.SOUND_PATH.shortName()).put(setting.sound.filePath));
        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.SOUND_URL.shortName()).put(setting.sound.url));

        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.VOLUME_FACTOR.shortName()).put(setting.volumeScalingFactor));
        updates.add(new AttributeUpdate(SleepSoundSettingAttributes.DATETIME.shortName()).put(setting.datetime.toString()));

        return updates;
    }

}

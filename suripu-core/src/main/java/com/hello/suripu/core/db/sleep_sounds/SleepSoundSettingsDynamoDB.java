package com.hello.suripu.core.db.sleep_sounds;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.SleepSoundSetting;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by ksg on 02/28/2017
*/
public class SleepSoundSettingsDynamoDB {
    private final static Logger LOGGER = LoggerFactory.getLogger(SleepSoundSettingsDynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;


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

    private Map<String, AttributeValue> getKey(final String senseId) {
        return ImmutableMap.of(SleepSoundSettingAttributes.SENSE_ID.shortName(), new AttributeValue().withS(senseId));
    }

    private Map<String, AttributeValue> getRangeKey(final Long accountId) {
        return ImmutableMap.of(SleepSoundSettingAttributes.ACCOUNT_ID.shortName(), new AttributeValue().withN(String.valueOf(accountId)));
    }

    public SleepSoundSettingsDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public CreateTableResult createTable(final Long readCapacityUnits, final Long writeCapacityUnits) {
        return Util.createTable(dynamoDBClient, tableName, SleepSoundSettingAttributes.SENSE_ID, SleepSoundSettingAttributes.ACCOUNT_ID, readCapacityUnits, writeCapacityUnits);
    }

    public Optional<SleepSoundSetting> get(final String senseId, final Long accountId) {
        final Map<String, AttributeValue> keys = Maps.newHashMap();
        keys.putAll(getKey(senseId));
        keys.putAll(getRangeKey(accountId));

        final GetItemRequest getRequest = new GetItemRequest()
                .withKey(keys)
                .withTableName(tableName);

        final GetItemResult result = dynamoDBClient.getItem(getRequest);
        final Map<String, AttributeValue> attributes = result.getItem();
        if (attributes != null && attributes.containsKey(SleepSoundSettingAttributes.ACCOUNT_ID.shortName())) {
            return Optional.of(toSoundSetting(attributes));
        }
        return Optional.absent();
    }

    /**
     * WARNING: Last write wins
     */
    public void update(final SleepSoundSetting setting) {
        final Map<String, AttributeValue> updateKeys = Maps.newHashMap();
        updateKeys.putAll(getKey(setting.senseId));
        updateKeys.putAll(getRangeKey(setting.accountId));

        final UpdateItemResult result = dynamoDBClient.updateItem(tableName, updateKeys, toUpdateItem(setting), "UPDATED_OLD");

        // getAttributes may be null or empty if this is the first time we've created this item or if nothing was changed, respectively.
        if ((result.getAttributes() != null) && (result.getAttributes().size() > 0)) {
            // Only log if something changed.
            final String loggableAttributes = Util.getLogFormattedAttributes(result.getAttributes());
            LOGGER.info("info=changed-sound-setting-attributes sense_id={} account_id={} {}", setting.senseId, setting.accountId, loggableAttributes);
        }
    }

    private SleepSoundSetting toSoundSetting(final Map<String, AttributeValue> item) {
        final Duration duration = Duration.create(
                Long.valueOf(item.get(SleepSoundSettingAttributes.DURATION_ID.shortName()).getN()),
                item.get(SleepSoundSettingAttributes.DURATION_NAME.shortName()).getS(),
                Integer.valueOf(item.get(SleepSoundSettingAttributes.DURATION_SECONDS.shortName()).getN()));

        final Sound sound = Sound.create(
                Long.valueOf(item.get(SleepSoundSettingAttributes.SOUND_ID.shortName()).getN()),
                item.get(SleepSoundSettingAttributes.SOUND_PREVIEW_URL.shortName()).getS(),
                item.get(SleepSoundSettingAttributes.SOUND_NAME.shortName()).getS(),
                item.get(SleepSoundSettingAttributes.SOUND_PATH.shortName()).getS(),
                item.get(SleepSoundSettingAttributes.SOUND_URL.shortName()).getS());

        final Integer volumeScalingFactor = Integer.valueOf(item.get(SleepSoundSettingAttributes.VOLUME_FACTOR.shortName()).getN());

        final String senseId = item.get(SleepSoundSettingAttributes.SENSE_ID.shortName()).getS();
        final Long accountId = Long.valueOf(item.get(SleepSoundSettingAttributes.ACCOUNT_ID.shortName()).getN());

        final DateTime dateTime = DateTime.parse(
                item.get(SleepSoundSettingAttributes.DATETIME.shortName()).getS()
        ).withZone(DateTimeZone.UTC);

        return SleepSoundSetting.create(senseId, accountId, dateTime, sound, duration, volumeScalingFactor);
    }


    private Map<String, AttributeValueUpdate> toUpdateItem(final SleepSoundSetting setting) {
        final Map<String, AttributeValueUpdate> updates = Maps.newHashMap();

        // updates.put(SleepSoundSettingAttributes.ACCOUNT_ID.shortName(), Util.putAction(setting.accountId));
        updates.put(SleepSoundSettingAttributes.DATETIME.shortName(), Util.putAction(setting.datetime.toString()));

        updates.put(SleepSoundSettingAttributes.DURATION_ID.shortName(), Util.putAction(setting.duration.id));
        updates.put(SleepSoundSettingAttributes.DURATION_NAME.shortName(), Util.putAction(setting.duration.name));
        updates.put(SleepSoundSettingAttributes.DURATION_SECONDS.shortName(), Util.putAction(setting.duration.durationSeconds.get()));

        updates.put(SleepSoundSettingAttributes.SOUND_ID.shortName(), Util.putAction(setting.sound.id));
        updates.put(SleepSoundSettingAttributes.SOUND_PREVIEW_URL.shortName(), Util.putAction(setting.sound.previewUrl));
        updates.put(SleepSoundSettingAttributes.SOUND_NAME.shortName(), Util.putAction(setting.sound.name));
        updates.put(SleepSoundSettingAttributes.SOUND_PATH.shortName(), Util.putAction(setting.sound.filePath));
        updates.put(SleepSoundSettingAttributes.SOUND_URL.shortName(), Util.putAction(setting.sound.url));

        updates.put(SleepSoundSettingAttributes.VOLUME_FACTOR.shortName(), Util.putAction(setting.volumeScalingFactor));

        return updates;
    }

}

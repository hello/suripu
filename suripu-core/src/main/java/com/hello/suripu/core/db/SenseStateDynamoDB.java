package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.input.State;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.SenseStateAtTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 2/19/16.
 *
 * Schema:
 *  sense_id (String, HK)
 *  timestamp (String)
 *  playing_audio (Bool)
 *  sleep_sound_duration_id (Number)
 *  sleep_sound_id (N)
 */
public class SenseStateDynamoDB {
    private final static Logger LOGGER = LoggerFactory.getLogger(SenseStateDynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public enum SenseStateAttribute implements Attribute {
        SENSE_ID("sense_id", "S"),  // Hash key
        TIMESTAMP("timestamp", "N"),
        PLAYING_AUDIO("playing_audio", "B"),
        SLEEP_SOUND_DURATION("duration", "N"),
        SLEEP_SOUND_FILE("sound", "S"),
        SLEEP_SOUND_VOLUME("volume", "N");

        private final String name;
        private final String type;

        SenseStateAttribute(String name, String type) {
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
        return ImmutableMap.of(SenseStateAttribute.SENSE_ID.shortName(), new AttributeValue().withS(senseId));
    }

    public SenseStateDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public CreateTableResult createTable(final Long readCapacityUnits, final Long writeCapacityUnits) {
        return Util.createTable(dynamoDBClient, tableName, SenseStateAttribute.SENSE_ID, readCapacityUnits, writeCapacityUnits);
    }

    private SenseStateAtTime toSenseState(final Map<String, AttributeValue> item) {
        final State.SenseState.Builder senseStateBuilder = State.SenseState.newBuilder()
                .setSenseId(item.get(SenseStateAttribute.SENSE_ID.shortName()).getS());

        if (item.containsKey(SenseStateAttribute.PLAYING_AUDIO.shortName())) {
            final State.AudioState.Builder audioStateBuilder = State.AudioState.newBuilder();
            final Boolean isPlayingAudio = item.get(SenseStateAttribute.PLAYING_AUDIO.shortName()).getBOOL();
            audioStateBuilder.setPlayingAudio(isPlayingAudio);
            if (isPlayingAudio) {
                audioStateBuilder.setDurationSeconds(Integer.valueOf(item.get(SenseStateAttribute.SLEEP_SOUND_DURATION.shortName()).getN()));
                audioStateBuilder.setFilePath(item.get(SenseStateAttribute.SLEEP_SOUND_FILE.shortName()).getS());
                if (item.containsKey(SenseStateAttribute.SLEEP_SOUND_VOLUME.shortName())) {
                    audioStateBuilder.setVolumePercent(Integer.valueOf(item.get(SenseStateAttribute.SLEEP_SOUND_VOLUME.shortName()).getN()));
                }
            }
            senseStateBuilder.setAudioState(audioStateBuilder.build());
        }

        final State.SenseState state = senseStateBuilder.build();

        final Long timestamp = Long.valueOf(item.get(SenseStateAttribute.TIMESTAMP.shortName()).getN());
        return new SenseStateAtTime(state, new DateTime(timestamp, DateTimeZone.UTC));
    }

    public Optional<SenseStateAtTime> getState(final String senseId) {
        final Map<String, AttributeValue> key = getKey(senseId);

        final Response<Optional<Map<String, AttributeValue>>> response = Util.getWithBackoff(dynamoDBClient, tableName, key);

        if (response.data.isPresent()) {
            return Optional.of(toSenseState(response.data.get()));
        }
        return Optional.absent();
    }

    private Map<String, AttributeValueUpdate> toDynamoDBUpdates(final SenseStateAtTime state) {
        final Map<String, AttributeValueUpdate> updates = Maps.newHashMap();
        updates.put(SenseStateAttribute.TIMESTAMP.shortName(), Util.putAction(state.timestamp.getMillis()));

        if (state.state.hasAudioState()) {
            final State.AudioState audioState = state.state.getAudioState();
            updates.put(SenseStateAttribute.PLAYING_AUDIO.shortName(), Util.putAction(audioState.getPlayingAudio()));

            if (audioState.getPlayingAudio()) {
                updates.put(SenseStateAttribute.SLEEP_SOUND_FILE.shortName(), Util.putAction(audioState.getFilePath()));
                updates.put(SenseStateAttribute.SLEEP_SOUND_DURATION.shortName(), Util.putAction((long) audioState.getDurationSeconds()));
                if (audioState.hasVolumePercent()) {
                    updates.put(SenseStateAttribute.SLEEP_SOUND_VOLUME.shortName(), Util.putAction((long) audioState.getVolumePercent()));
                }
            }
        }

        return updates;
    }

    /**
     * WARNING: Last write wins
     */
    public void updateState(final SenseStateAtTime state) {
        final UpdateItemResult result = dynamoDBClient.updateItem(tableName, getKey(state.state.getSenseId()), toDynamoDBUpdates(state), "UPDATED_OLD");

        // getAttributes may be null or empty if this is the first time we've created this item or if nothing was changed, respectively.
        if ((result.getAttributes() != null) && (result.getAttributes().size() > 0)) {
            // Only log if something changed.
            final String loggableAttributes = Util.getLogFormattedAttributes(result.getAttributes());
            LOGGER.info("info=changed-sense-state-attributes sense-id={} {}", state.state.getSenseId(), loggableAttributes);
        }
    }

}

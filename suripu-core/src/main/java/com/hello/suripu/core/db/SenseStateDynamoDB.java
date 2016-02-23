package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hello.suripu.api.input.State;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.models.SenseStateAtTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
        SLEEP_SOUND_FILE("sound", "S");

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
    }

    private Map<String, AttributeValue> getKey(final String senseId) {
        return ImmutableMap.of(SenseStateAttribute.SENSE_ID.shortName(), new AttributeValue().withS(senseId));
    }

    public SenseStateDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public CreateTableResult createTable(final Long readCapacityUnits, final Long writeCapacityUnits) {
        // attributes
        final SenseStateAttribute hashKey = SenseStateAttribute.SENSE_ID;
        List<AttributeDefinition> attributes = ImmutableList.of(
                new AttributeDefinition().withAttributeName(hashKey.shortName()).withAttributeType(hashKey.type)
        );

        // Keys
        List<KeySchemaElement> keySchema = ImmutableList.of(
                new KeySchemaElement().withAttributeName(hashKey.shortName()).withKeyType(KeyType.HASH)
        );

        // throughput provision
        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(readCapacityUnits)
                .withWriteCapacityUnits(writeCapacityUnits);

        final CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributes)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(provisionedThroughput);

        return dynamoDBClient.createTable(request);
    }

    private void backoff(int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            LOGGER.warn("Throttled by DynamoDB, sleeping for {} ms.", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while attempting exponential backoff.");
        }
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
            }
            senseStateBuilder.setAudioState(audioStateBuilder.build());
        }

        final State.SenseState state = senseStateBuilder.build();

        final Long timestamp = Long.valueOf(item.get(SenseStateAttribute.TIMESTAMP.shortName()).getN());
        return new SenseStateAtTime(state, new DateTime(timestamp, DateTimeZone.UTC));
    }

    public Optional<SenseStateAtTime> getState(final String senseId) {
        final Map<String, AttributeValue> key = getKey(senseId);

        GetItemResult result = null;
        int numAttempts = 0;

        do {
            numAttempts++;
            try {
                result = dynamoDBClient.getItem(tableName, key);
            } catch (ResourceNotFoundException rnfe) {
                return Optional.absent();
            } catch (ProvisionedThroughputExceededException ptee) {
                if (numAttempts < 5) {
                    backoff(numAttempts);
                } else {
                    LOGGER.error("error=ProvisionedThroughputExceededException tries=% status=aborted", numAttempts);
                    return Optional.absent();
                }
            }
        } while (result == null);

        if (result.getItem() == null) {
            return Optional.absent();
        }
        return Optional.of(toSenseState(result.getItem()));
    }

    private AttributeValueUpdate putAction(final Long value) {
        return new AttributeValueUpdate(new AttributeValue().withN(String.valueOf(value)), "PUT");
    }

    private AttributeValueUpdate putAction(final String value) {
        return new AttributeValueUpdate(new AttributeValue().withS(value), "PUT");
    }

    private AttributeValueUpdate putAction(final Boolean value) {
        return new AttributeValueUpdate(new AttributeValue().withBOOL(value), "PUT");
    }

    private Map<String, AttributeValueUpdate> toDynamoDBUpdates(final SenseStateAtTime state) {
        final Map<String, AttributeValueUpdate> updates = Maps.newHashMap();
        updates.put(SenseStateAttribute.TIMESTAMP.shortName(), putAction(state.timestamp.getMillis()));

        if (state.state.hasAudioState()) {
            final State.AudioState audioState = state.state.getAudioState();
            updates.put(SenseStateAttribute.PLAYING_AUDIO.shortName(), putAction(audioState.getPlayingAudio()));

            if (audioState.getPlayingAudio()) {
                updates.put(SenseStateAttribute.SLEEP_SOUND_FILE.shortName(), putAction(audioState.getFilePath()));
                updates.put(SenseStateAttribute.SLEEP_SOUND_DURATION.shortName(), putAction((long) audioState.getDurationSeconds()));
            }
        }

        return updates;
    }

    /**
     * WARNING: Last write wins
     * @param state
     */
    public void updateState(final SenseStateAtTime state) {
        dynamoDBClient.updateItem(tableName, getKey(state.state.getSenseId()), toDynamoDBUpdates(state));
    }

}

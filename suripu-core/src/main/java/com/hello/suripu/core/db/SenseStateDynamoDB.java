package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.api.input.State;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Expressions;
import com.hello.suripu.core.db.dynamo.expressions.Expression;
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
        TIMESTAMP("timestamp", "S"),
        PLAYING_AUDIO("playing_audio", "B"),
        SLEEP_SOUND_DURATION_ID("duration", "N"),
        SLEEP_SOUND_ID("sound", "N");

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

    private State.SenseState toSenseState(final Map<String, AttributeValue> item) {
        final State.AudioState.Builder audioStateBuilder = State.AudioState.newBuilder();
        final Boolean isPlayingAudio = item.get(SenseStateAttribute.PLAYING_AUDIO.shortName()).getBOOL();
        audioStateBuilder.setPlayingAudio(isPlayingAudio);
        if (isPlayingAudio) {
            // TODO
        }

        return State.SenseState.newBuilder()
                .setAudioState(audioStateBuilder.build())
                .setSenseId(item.get(SenseStateAttribute.SENSE_ID.shortName()).getS())
                .build();
    }

    public Optional<State.SenseState> getState(final String senseId) {
        final Map<String, AttributeValue> key = ImmutableMap.of(SenseStateAttribute.SENSE_ID.shortName(), new AttributeValue().withS(senseId));

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

        return Optional.of(toSenseState(result.getItem()));
    }
}

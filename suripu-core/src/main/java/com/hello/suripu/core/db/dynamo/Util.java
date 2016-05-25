package com.hello.suripu.core.db.dynamo;

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
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.responses.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 3/9/16.
 */
public class Util {
    private final static Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private static void backoff(final String tableName, int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            LOGGER.warn("reason=dynamodb-throttling sleep-millis={} table={}", sleepMillis, tableName);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOGGER.error("error=interrupted-exception table={} exception={}", tableName, e);
        }
    }

    public static Response<Optional<Map<String, AttributeValue>>> getWithBackoff(final AmazonDynamoDB dynamoDB, final String tableName, final Map<String, AttributeValue> key) {
        GetItemResult result = null;
        int numAttempts = 0;

        do {
            numAttempts++;
            try {
                result = dynamoDB.getItem(tableName, key, true);
            } catch (ResourceNotFoundException rnfe) {
                return Response.failure(Optional.<Map<String,AttributeValue>>absent(), rnfe);
            } catch (ProvisionedThroughputExceededException ptee) {
                if (numAttempts < 5) {
                    backoff(tableName, numAttempts);
                } else {
                    LOGGER.error("error=ProvisionedThroughputExceededException tries={} status=aborted table={}",
                            numAttempts, tableName);
                    return Response.failure(Optional.<Map<String,AttributeValue>>absent(), ptee);
                }
            }
        } while (result == null);

        if (result.getItem() == null) {
            return Response.failure(Optional.<Map<String,AttributeValue>>absent());
        }
        return Response.success(Optional.of(result.getItem()));
    }


    // region Table creation
    private static CreateTableResult createTable(final AmazonDynamoDB dynamoDB, final String tableName,
                                                 final List<AttributeDefinition> attributeDefinitions,
                                                 final List<KeySchemaElement> keySchema,
                                                 final Long readCapacityUnits, final Long writeCapacityUnits)
    {
        // throughput provision
        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(readCapacityUnits)
                .withWriteCapacityUnits(writeCapacityUnits);

        final CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(provisionedThroughput);

        return dynamoDB.createTable(request);
    }

    /**
     * Create a table with the given hash key.
     */
    public static CreateTableResult createTable(final AmazonDynamoDB dynamoDB, final String tableName, final Attribute hashKey,
                                                final Long readCapacityUnits, final Long writeCapacityUnits)
    {
        List<AttributeDefinition> attributes = ImmutableList.of(
                new AttributeDefinition().withAttributeName(hashKey.shortName()).withAttributeType(hashKey.type())
        );

        // Keys
        List<KeySchemaElement> keySchema = ImmutableList.of(
                new KeySchemaElement().withAttributeName(hashKey.shortName()).withKeyType(KeyType.HASH)
        );

        return createTable(dynamoDB, tableName, attributes, keySchema, readCapacityUnits, writeCapacityUnits);
    }

    /**
     * Create a table with the given hash and range keys.
     */
    public static CreateTableResult createTable(final AmazonDynamoDB dynamoDB, final String tableName,
                                                final Attribute hashKey, final Attribute rangeKey,
                                                final Long readCapacityUnits, final Long writeCapacityUnits)
    {
        List<AttributeDefinition> attributes = ImmutableList.of(
                new AttributeDefinition().withAttributeName(hashKey.shortName()).withAttributeType(hashKey.type()),
                new AttributeDefinition().withAttributeName(rangeKey.shortName()).withAttributeType(rangeKey.type())
        );

        // Keys
        List<KeySchemaElement> keySchema = ImmutableList.of(
                new KeySchemaElement().withAttributeName(hashKey.shortName()).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(rangeKey.shortName()).withKeyType(KeyType.RANGE)
        );

        return createTable(dynamoDB, tableName, attributes, keySchema, readCapacityUnits, writeCapacityUnits);
    }
    // endregion Table creation


    // region AttributeValueUpdate
    public static AttributeValueUpdate putAction(final ByteBuffer value) {
        return new AttributeValueUpdate(new AttributeValue().withB(value), "PUT");
    }

    public static AttributeValueUpdate putAction(final byte[] value) {
        return putAction(ByteBuffer.wrap(value));
    }

    public static AttributeValueUpdate putAction(final Long value) {
        return new AttributeValueUpdate(new AttributeValue().withN(String.valueOf(value)), "PUT");
    }

    public static AttributeValueUpdate putAction(final Integer value) {
        return new AttributeValueUpdate(new AttributeValue().withN(String.valueOf(value)), "PUT");
    }

    public static AttributeValueUpdate putAction(final String value) {
        return new AttributeValueUpdate(new AttributeValue().withS(value), "PUT");
    }

    public static AttributeValueUpdate putAction(final Boolean value) {
        return new AttributeValueUpdate(new AttributeValue().withBOOL(value), "PUT");
    }

    // number set
    public static AttributeValueUpdate addToSetAction(final Integer value) {
        return new AttributeValueUpdate(new AttributeValue().withNS(String.valueOf(value)), "ADD");
    }

    // endregion AttributeValueUpdate


    /**
     * Format Dynamo attribute map as key=value log pairs.
     * NOTE: Only handles the following attribute types currently: N, S, Bool, B
     * @param attributes Attributes to format.
     * @return String with key=value log pairs.
     */
    public static String getLogFormattedAttributes(final Map<String, AttributeValue> attributes) {
        final List<String> parts = Lists.newArrayList();
        for (final Map.Entry<String, AttributeValue> entry : attributes.entrySet()) {
            final AttributeValue value = entry.getValue();
            final String valueString;
            if (value.getN() != null) {
                valueString = value.getN();
            } else if (value.getS() != null) {
                valueString = value.getS();
            } else if (value.getBOOL() != null) {
                valueString = value.getBOOL().toString();
            } else if (value.getB() != null) {
                valueString = new String(value.getB().array());
            } else {
                // Don't handle other (nested) types for now.
                valueString = "UNKOWN";
            }
            final String part = entry.getKey() + "=" + valueString;
            parts.add(part);
        }
        return Joiner.on(" ").join(parts);
    }
}

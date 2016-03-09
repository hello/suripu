package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.db.responses.SingleItemDynamoDBResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by jakepiccolo on 3/8/16.
 */
public class FileManifestDynamoDB implements FileManifestDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileManifestDynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;


    public enum FileManifestAttribute implements Attribute {
        SENSE_ID("sense_id", "S"),  // Hash key
        LATEST_FILE_MANIFEST("manifest", "N");

        private final String name;
        private final String type;

        FileManifestAttribute(String name, String type) {
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
        return ImmutableMap.of(FileManifestAttribute.SENSE_ID.shortName(), new AttributeValue().withS(senseId));
    }


    public FileManifestDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public CreateTableResult createTable(final Long readCapacity, final Long writeCapacity) {
        return Util.createTable(dynamoDBClient, tableName, FileManifestAttribute.SENSE_ID, readCapacity, writeCapacity);
    }

    @Override
    public Optional<FileManifest> updateManifest(FileManifest newManifest) {
        return Optional.absent();
    }

    private FileManifest toFileManifest(final Map<String, AttributeValue> item) {
        // TODO
        return null;
    }

    @Override
    public Optional<FileManifest> getManifest(String senseId) {
        final Map<String, AttributeValue> key = getKey(senseId);

        final SingleItemDynamoDBResponse response = Util.getWithBackoff(dynamoDBClient, tableName, key);

        if (response.data.isPresent()) {
            return Optional.of(toFileManifest(response.data.get()));
        }
        return Optional.absent();
    }
}

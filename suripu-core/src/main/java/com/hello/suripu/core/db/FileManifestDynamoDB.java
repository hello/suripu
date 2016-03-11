package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.FileSync;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.db.responses.Response;
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
        LATEST_FILE_MANIFEST("manifest", "B");

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




    public FileManifestDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public CreateTableResult createTable(final Long readCapacity, final Long writeCapacity) {
        return Util.createTable(dynamoDBClient, tableName, FileManifestAttribute.SENSE_ID, readCapacity, writeCapacity);
    }

    // region FileManifestDAO overrides
    @Override
    public Optional<FileSync.FileManifest> updateManifest(final String senseId, FileSync.FileManifest newManifest) {
        final Map<String, AttributeValueUpdate> updates = ImmutableMap.of(
                FileManifestAttribute.SENSE_ID.shortName(), Util.putAction(senseId),
                FileManifestAttribute.LATEST_FILE_MANIFEST.shortName(), Util.putAction(newManifest.toByteArray())
        );

        final UpdateItemResult result = dynamoDBClient.updateItem(tableName, getKey(senseId), updates, "ALL_OLD");

        if (result.getAttributes() != null) {
            if (result.getAttributes().containsKey(FileManifestAttribute.LATEST_FILE_MANIFEST.shortName())) {
                try {
                    return Optional.of(toFileManifest(result.getAttributes()));
                } catch (InvalidProtocolBufferException e) {
                    LOGGER.error("error=InvalidProtocolBufferException sense-id={} exception={}", senseId, e);
                }
            }
        }

        LOGGER.warn("error=no-old-file-manifest sense-id={}", senseId);
        return Optional.absent();
    }

    @Override
    public Optional<FileSync.FileManifest> getManifest(String senseId) {
        final Map<String, AttributeValue> key = getKey(senseId);

        final Response<Optional<Map<String, AttributeValue>>> response = Util.getWithBackoff(dynamoDBClient, tableName, key);

        if (response.data.isPresent()) {
            try {
                return Optional.of(toFileManifest(response.data.get()));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("error=InvalidProtocolBufferException sense-id={} exception={}", senseId, e);
            }
        }
        LOGGER.warn("error=no-file-manifest sense-id={}", senseId);
        return Optional.absent();
    }
    // endregion FileManifestDAO overrides


    // region private helpers
    private FileSync.FileManifest toFileManifest(final Map<String, AttributeValue> item) throws InvalidProtocolBufferException {
        final byte[] serializedManifest = item.get(FileManifestAttribute.LATEST_FILE_MANIFEST.shortName()).getB().array();
        return FileSync.FileManifest.parseFrom(serializedManifest);
    }

    private Map<String, AttributeValue> getKey(final String senseId) {
        return ImmutableMap.of(FileManifestAttribute.SENSE_ID.shortName(), new AttributeValue().withS(senseId));
    }
    // endregion private helpers
}

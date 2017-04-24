package com.hello.suripu.core.sense.metadata;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import com.hello.suripu.core.models.device.v2.Sense;
import com.hello.suripu.core.provision.SerialNumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a wrapper around the keystore to extract some metadata without exposing keys
 */
public class MetadataDAODynamoDB implements SenseMetadataDAO {

    private static Logger LOGGER = LoggerFactory.getLogger(MetadataDAODynamoDB.class);

    private final KeyStore keyStore;

    private MetadataDAODynamoDB(final KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public static MetadataDAODynamoDB create(final KeyStore keyStore) {
        return new MetadataDAODynamoDB(keyStore);

    }
    @Override
    public SenseMetadata get(String senseId) {

        final Optional<DeviceKeyStoreRecord> recordFromDDB = keyStore.getKeyStoreRecord(senseId);
        if(!recordFromDDB.isPresent()) {
            return SenseMetadata.unknown(senseId);
        }

        final DeviceKeyStoreRecord record = recordFromDDB.get();
        final Optional<Sense.Color> colorOptional = SerialNumberUtils.extractColorFrom(record.metadata);
        return SenseMetadata.create(senseId, colorOptional.or(Sense.DEFAULT_COLOR), record.hardwareVersion);
    }

    @Override
    public Integer put(final SenseMetadata senseMetadata) {
        return 0;
    }
}

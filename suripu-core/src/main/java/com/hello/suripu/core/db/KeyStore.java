package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Set;

public interface KeyStore {

    /**
     * Retrieves a public key (in bytes) from a store
     * @param deviceId String : unique identifier representing a device
     * @return byte[]
     */
    Optional<byte[]> get(String deviceId);
    Optional<byte[]> getStrict(String deviceId);

    Optional<DeviceKeyStoreRecord> getKeyStoreRecord(String deviceId);


    /**
     *
     * @param deviceId String : unique identifier representing a device
     * @param aesKey String
     */
    void put(String deviceId, String aesKey);
    void put(String deviceId, String aesKey, String serialNumber);
    void put(String deviceId, String aesKey, String serialNumber, DateTime createdAt);


    Map<String, Optional<byte[]>> getBatch(Set<String> deviceIds);
}

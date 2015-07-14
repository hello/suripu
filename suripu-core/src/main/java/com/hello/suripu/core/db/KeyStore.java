package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;

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
     * @param publicKey String: base64 encoded string representing a public key generated at the factory
     */
    void put(String deviceId, String publicKey);
    void put(String deviceId, String publicKey, String metadata);


    Map<String, Optional<byte[]>> getBatch(Set<String> deviceIds);
}

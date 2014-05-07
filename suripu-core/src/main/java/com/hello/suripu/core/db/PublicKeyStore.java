package com.hello.suripu.core.db;

import com.google.common.base.Optional;

public interface PublicKeyStore {

    /**
     * Retrieves a public key (in bytes) from a store
     * @param deviceId String : unique identifier representing a device
     * @return byte[]
     */
    Optional<byte[]> get(String deviceId);


    /**
     *
     * @param deviceId String : unique identifier representing a device
     * @param publicKey String: base64 encoded string representing a public key generated at the factory
     */
    void put(String deviceId, String publicKey);
}

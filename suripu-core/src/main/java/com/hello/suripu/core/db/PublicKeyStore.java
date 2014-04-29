package com.hello.suripu.core.db;

public interface PublicKeyStore {

    /**
     * Retrieves a public key (in bytes) from a store
     * @param deviceId String : unique identifier representing a device
     * @return byte[]
     */
    byte[] get(String deviceId);


    /**
     *
     * @param deviceId String : unique identifier representing a device
     * @param publicKey String: base64 encoded string representing a public key generated at the factory
     */
    void put(String deviceId, String publicKey);
}

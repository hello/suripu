package com.hello.suripu.core.db;

public interface PublicKeyStore {

    byte[] get(String deviceId);
    void put(String deviceId, String publicKey);
}

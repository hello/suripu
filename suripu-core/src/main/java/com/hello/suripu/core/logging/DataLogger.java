package com.hello.suripu.core.logging;

public interface DataLogger {
    void putAsync(String deviceId, byte[] payload);

    String put(String deviceId, byte[] payload);

    String putWithSequenceNumber(String deviceId, byte[] payload, String sequenceNumber);
}

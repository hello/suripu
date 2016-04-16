package com.hello.suripu.core.logging;

/**
 * Created by ksg on 4/13/16
 */
public class DataLoggerBatchPayload {
    public final String deviceId;
    public final byte[] payload;

    public DataLoggerBatchPayload(final String deviceId, final byte[] payload) {
        this.deviceId = deviceId;
        this.payload = payload;
    }
}

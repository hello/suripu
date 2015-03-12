package com.hello.suripu.core.models;

/**
 * Created by jnorgan on 3/5/15.
 */
public class FirmwareInfo {
    public final String version;
    public final String device_id;
    public final Long timestamp;


    public FirmwareInfo(final String version, final String device_id, final Long timestamp) {
        this.version = version;
        this.device_id = device_id;
        this.timestamp = timestamp;
    }
}
package com.hello.suripu.core.models;

/**
 * Created by jnorgan on 3/10/15.
 */
public class FirmwareCountInfo {
    public final String version;
    public final Long count;

    public FirmwareCountInfo(final String version, final Long count) {
        this.version = version;
        this.count = count;
    }
}

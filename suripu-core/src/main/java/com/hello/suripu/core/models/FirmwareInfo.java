package com.hello.suripu.core.models;

import java.util.List;
import java.util.Set;

/**
 * Created by jnorgan on 3/5/15.
 */
public class FirmwareInfo {
    public final String version;
    public final Set<String> deviceIds;


    public FirmwareInfo(String version, Set<String> deviceIds) {
        this.version = version;
        this.deviceIds = deviceIds;
    }
}

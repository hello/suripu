package com.hello.suripu.core.models;

import java.util.List;
import org.joda.time.DateTime;

/**
 * Created by jnorgan on 4/21/15.
 */
public class OTAHistory {
    public final String deviceId;
    public final DateTime eventTime;
    public final String currentFWVersion;
    public final String newFWVersion;
    public final List<String> fileList;

    public OTAHistory(final String deviceId, final DateTime eventTime, final String currentFW, final String newFW, final List<String> urlList) {
        this.deviceId = deviceId;
        this.eventTime = eventTime;
        this.currentFWVersion = currentFW;
        this.newFWVersion = newFW;
        this.fileList = urlList;
    }

}

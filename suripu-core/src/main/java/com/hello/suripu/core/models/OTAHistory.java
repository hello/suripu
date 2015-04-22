package com.hello.suripu.core.models;

import java.util.List;
import org.joda.time.DateTime;

/**
 * Created by jnorgan on 4/21/15.
 */
public class OTAHistory {
    public final String deviceId;
    public final DateTime eventTime;
    public final Integer currentFWVersion;
    public final Integer newFWVersion;
    public final List<String> fileList;

    public OTAHistory(final String deviceId, final DateTime eventTime, final Integer currentFW, final Integer newFW, final List<String> urlList) {
        this.deviceId = deviceId;
        this.eventTime = eventTime;
        this.currentFWVersion = currentFW;
        this.newFWVersion = newFW;
        this.fileList = urlList;
    }

}

package com.hello.suripu.core.models;

import com.hello.suripu.core.ota.Status;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by jnorgan on 4/21/15.
 */
public class OTAHistory {

    public final String deviceId;
    public final DateTime eventTime;
    public final String currentFWVersion;
    public final String newFWVersion;
    public final List<String> fileList;
    public final Status otaStatus;

    public OTAHistory(
        final String deviceId,
        final DateTime eventTime,
        final String currentFW,
        final String newFW,
        final List<String> urlList,
        final Status otaStatus) {

        this.deviceId = deviceId;
        this.eventTime = eventTime;
        this.currentFWVersion = currentFW;
        this.newFWVersion = newFW;
        this.fileList = urlList;
        this.otaStatus = otaStatus;

    }

}

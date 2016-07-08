package com.hello.suripu.core.models;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by jnorgan on 4/21/15.
 */
public class OTAHistory {

    public enum OTAStatus {
        UNKNOWN,
        UPDATE_REQUIRED,
        RESPONSE_SENT,
        IN_PROGRESS,
        COMPLETE;

        public static OTAStatus fromString(final String text) {
            if (text == null) {
                return OTAStatus.UNKNOWN;
            }

            for (final OTAStatus status : OTAStatus.values()) {
                if (text.equalsIgnoreCase(status.toString())) {
                    return status;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public final String deviceId;
    public final DateTime eventTime;
    public final String currentFWVersion;
    public final String newFWVersion;
    public final List<String> fileList;
    public final OTAStatus otaStatus;

    public OTAHistory(
        final String deviceId,
        final DateTime eventTime,
        final String currentFW,
        final String newFW,
        final List<String> urlList,
        final OTAStatus otaStatus) {

        this.deviceId = deviceId;
        this.eventTime = eventTime;
        this.currentFWVersion = currentFW;
        this.newFWVersion = newFW;
        this.fileList = urlList;
        this.otaStatus = otaStatus;

    }

}

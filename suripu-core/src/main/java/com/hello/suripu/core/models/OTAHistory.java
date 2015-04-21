package com.hello.suripu.core.models;

import java.util.List;

/**
 * Created by jnorgan on 4/21/15.
 */
public class OTAHistory {
    public final String deviceId;
    public final Long eventTime;
    public final Integer currentFW;
    public final Integer newFW;
    public final List<String> fileList;

    public OTAHistory(final String device_id, final Long event_time, final Integer current_fw, final Integer new_fw, final List<String> url_list) {
        this.deviceId = device_id;
        this.eventTime = event_time;
        this.currentFW = current_fw;
        this.newFW = new_fw;
        this.fileList = url_list;
    }

}

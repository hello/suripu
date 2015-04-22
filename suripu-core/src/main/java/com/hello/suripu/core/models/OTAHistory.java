package com.hello.suripu.core.models;

import java.util.List;

/**
 * Created by jnorgan on 4/21/15.
 */
public class OTAHistory {
    public final String device_id;
    public final String event_time;
    public final Integer current_fw_version;
    public final Integer new_fw_version;
    public final List<String> file_list;

    public OTAHistory(final String device_id, final String event_time, final Integer current_fw, final Integer new_fw, final List<String> url_list) {
        this.device_id = device_id;
        this.event_time = event_time;
        this.current_fw_version = current_fw;
        this.new_fw_version = new_fw;
        this.file_list = url_list;
    }

}

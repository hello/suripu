package com.hello.suripu.core.speech;

import com.hello.suripu.core.db.dynamo.Attribute;

/**
 * Created by ksg on 8/24/16
 */
public enum SpeechTimelineAttribute implements Attribute {
    ACCOUNT_ID("account_id", "N", ":aid"),      // Hash Key (account-id)
    TS("ts", "S", ":ts"),                       // Range Key (timestamp in UTC)
    SENSE_ID("sense_id", "S", ":sid"),          // sense-id
    ENCRYPTED_UUID("e_uuid", "S", ":euuid");    // encrypted UUID to audio identifier

    private final String name;
    private final String type;
    private final String queryHolder;

    SpeechTimelineAttribute(String name, String type, String queryHolder) {
        this.name = name;
        this.type = type;
        this.queryHolder = queryHolder;
    }

    public String sanitizedName() {
        return toString();
    }
    public String shortName() {
        return name;
    }
    public String type() {
        return type;
    }

    public String queryHolder() { return queryHolder; }
}

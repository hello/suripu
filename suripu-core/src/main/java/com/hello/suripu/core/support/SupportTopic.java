package com.hello.suripu.core.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SupportTopic {

    @JsonIgnore
    public final Integer id;

    @JsonProperty("topic")
    private final String topic;

    @JsonProperty("display_name")
    private final String displayName;


    private SupportTopic(final Integer id, final String topic, final String displayName) {
        this.id = null;
        this.topic = topic;
        this.displayName = displayName;
    }

    public static SupportTopic create(final Integer id, final String topic, final String displayName) {
        return new SupportTopic(id, topic, displayName);
    }
}

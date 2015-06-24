package com.hello.suripu.core.models;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.util.Map;

public class SenseLogDocument {
    public final String senseId;
    public final Long timestamp;
    public final String content;
    public final String origin;
    public SenseLogDocument(final String senseId, final Long timestamp, final String content, final String origin) {
        this.senseId = senseId;
        this.timestamp = timestamp;
        this.content = content;
        this.origin =  origin;
    }
    public Map<String, Object> toMap() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return objectMapper.convertValue(this, Map.class);
    }
}

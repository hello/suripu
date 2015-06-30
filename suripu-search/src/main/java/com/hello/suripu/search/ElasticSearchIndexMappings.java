package com.hello.suripu.search;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.common.base.Optional;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class ElasticSearchIndexMappings {
    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchIndexMappings.class);
    private final static String INDEX_TIME_TO_LIVE_SETTINGS_KEY = "_ttl";
    private final static String INDEX_TIME_TO_LIVE_ENABLE_KEY = "enabled";
    private final static String INDEX_TIME_TO_LIVE_DURATION_KEY = "default";
    private final static String DEFAULT_ANALYZER = "sense_logs_analyzer";
    private final static Boolean DEFAULT_TIME_TO_LIVE_ENABLED = Boolean.FALSE;
    private final static Long DEFAULT_TIME_TO_LIVE_MILLIS = Long.MAX_VALUE;
    private final static Boolean IMMORTAL_TIME_TO_LIVE_ENABLED = Boolean.FALSE;
    private final static Long IMMORTAL_TIME_TO_LIVE_MILLIS = Long.MAX_VALUE;

    public final Boolean timeToLiveEnabled;
    public final Long timeToLiveMillis;

    public ElasticSearchIndexMappings(final Boolean timeToLiveEnabled, final Long timeToLiveMillis) {
        this.timeToLiveEnabled = timeToLiveEnabled;
        this.timeToLiveMillis = timeToLiveMillis;
    }

    public static ElasticSearchIndexMappings createDefault() {
        return new ElasticSearchIndexMappings(DEFAULT_TIME_TO_LIVE_ENABLED, DEFAULT_TIME_TO_LIVE_MILLIS);
    }

    public static ElasticSearchIndexMappings createImmortal() {
        return new ElasticSearchIndexMappings(IMMORTAL_TIME_TO_LIVE_ENABLED, IMMORTAL_TIME_TO_LIVE_MILLIS);
    }

    public Optional<XContentBuilder> toJSON() {
        try {
            return Optional.of(XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject(INDEX_TIME_TO_LIVE_SETTINGS_KEY)
                    .field(INDEX_TIME_TO_LIVE_ENABLE_KEY, timeToLiveEnabled)
                    .field(INDEX_TIME_TO_LIVE_DURATION_KEY, timeToLiveMillis)
                    .startObject("_timestamp")
                    .endObject()
                    .startObject("properties")
                    .startObject("content")
                    .field("type", "string")
                    .field("analyzer", DEFAULT_ANALYZER)
                    .endObject()
                    .endObject()
                    .endObject());
        }
        catch (IOException e) {
            LOGGER.error("Failed to add serialize mapping because {}", e.getMessage());
            return Optional.absent();
        }
    }
    public JSONObject toJSONObject() {
        try {
            return new JSONObject()
                .put(INDEX_TIME_TO_LIVE_SETTINGS_KEY, new JSONObject()
                    .put(INDEX_TIME_TO_LIVE_ENABLE_KEY, timeToLiveEnabled)
                    .put(INDEX_TIME_TO_LIVE_DURATION_KEY, timeToLiveMillis)
                )
                .put("properties", new JSONObject()
                    .put("content", new JSONObject()
                        .put("type", "string")
                        .put("analyzer", DEFAULT_ANALYZER)
                    )
                );
        }
        catch (JSONException e) {
            LOGGER.error("Failed to add index mappings because {}", e.getMessage());
            return new JSONObject();
        }
    }
}

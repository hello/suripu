package com.hello.suripu.search;


import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.common.base.Optional;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ElasticSearchIndexSettings {
    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchIndexSettings.class);
    private final static Integer DEFAULT_NUMBER_OF_SHARDS = 5;
    private final static Integer DEFAULT_NUMBER_OF_REPLICAS = 1;
    private final static String DEFAULT_FILTER_TYPE = "word_delimiter";
    private final static String DEFAULT_ANALYZER_TYPE = "custom";
    private final static String DEFAULT_TOKENIZER = "whitespace";
    private final static String DEFAULT_FILTER_NAME = "sense_logs_filter";
    private final static String DEFAULT_ANALYZER_NAME = "sense_logs_analyzer";
    private final static String[] DEFAULT_FILTER_ARRAY = new String[]{"lowercase", DEFAULT_FILTER_NAME};
    private final static String[] DEFAULT_FILTER_TYPE_TABLE = new String[]{
            "# => ALPHANUM",
            "@ => ALPHANUM",
            ": => ALPHANUM",
            "- => ALPHANUM",
            "+ => ALPHANUM",
            "/ => ALPHANUM",
    };

    public final Integer numberOfShards;
    public final Integer numberOfReplicas;
    public final String filterType;
    public final String[] filterTypeTable;
    public final String analyzerType;
    public final String tokenizer;

    public ElasticSearchIndexSettings(final Integer numberOfShards,
                                      final Integer numberOfReplicas,
                                      final String filterType,
                                      final String[] filterTypeTable,
                                      final String analyzerType,
                                      final String tokenizer){
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
        this.filterType = filterType;
        this.filterTypeTable = filterTypeTable;
        this.analyzerType = analyzerType;
        this.tokenizer = tokenizer;
    }

    public static ElasticSearchIndexSettings createDefault() {
        return new ElasticSearchIndexSettings(DEFAULT_NUMBER_OF_SHARDS, DEFAULT_NUMBER_OF_REPLICAS, DEFAULT_FILTER_TYPE, DEFAULT_FILTER_TYPE_TABLE, DEFAULT_ANALYZER_TYPE, DEFAULT_TOKENIZER);
    }

    public Optional<String> toJSONString() {
        try {
            return Optional.of(XContentFactory.jsonBuilder()
                .startObject()
                    .field("number_of_shards", numberOfShards)
                        .field("number_of_replicas", numberOfReplicas)
                    .startObject("analysis")
                    .startObject("filter")
                    .startObject(DEFAULT_FILTER_NAME)
                    .field("type", DEFAULT_FILTER_TYPE)
                                .field("type_table", DEFAULT_FILTER_TYPE_TABLE)
                    .endObject()
                    .endObject()
                    .startObject("analyzer")
                    .startObject(DEFAULT_ANALYZER_NAME)
                    .field("type", analyzerType)
                                .field("tokenizer", tokenizer)
                                .field("filter", DEFAULT_FILTER_ARRAY)
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject().string());
        }
        catch (IOException e) {
            LOGGER.error("Failed to add settings because {}", e.getMessage());
            return Optional.absent();
        }
    }

    public JSONObject toJSONObject() {
        try {
            return new JSONObject()
                .put("number_of_shards", numberOfShards)
                .put("number_of_replicas", numberOfReplicas)
                .put("analysis", new JSONObject()
                    .put("filter", new JSONObject()
                        .put(DEFAULT_FILTER_NAME, new JSONObject()
                            .put("type", DEFAULT_FILTER_TYPE)
                            .put("type_table", DEFAULT_FILTER_TYPE_TABLE)
                        )
                    )
                    .put("analyzer", new JSONObject()
                        .put(DEFAULT_ANALYZER_NAME, new JSONObject()
                            .put("type", analyzerType)
                            .put("tokenizer", tokenizer)
                            .put("filter", DEFAULT_FILTER_ARRAY)
                        )
                    )
                );
        }
        catch (JSONException e) {
            LOGGER.error("Failed to add settings because {}", e.getMessage());
            return new JSONObject();
        }

    }
}

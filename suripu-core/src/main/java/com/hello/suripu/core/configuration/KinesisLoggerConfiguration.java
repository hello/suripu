package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class KinesisLoggerConfiguration {

    @JsonProperty("stream_name")
    private String streamName;

    public String getStreamName() {
        return streamName;
    }

    @JsonProperty("enabled")
    private Boolean enabled = false;

    public Boolean isEnabled() {
        return enabled;
    }

    @JsonProperty("buffer_size")
    @Max(200)
    @Min(10)
    private Integer bufferSize = 50;

    public Integer bufferSize() {
        return bufferSize;
    }

    @JsonProperty("origin")
    private String origin;

    public String origin() {
        return origin;
    }

    @JsonProperty("production")
    private Boolean isProduction = false;
    public Boolean isProduction() {return isProduction;}

    @JsonProperty("min_log_level")
    private String logLevel = "DEBUG";
    public String getLogLevel() {
        return logLevel;
    }

}

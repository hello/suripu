package com.hello.suripu.coredropwizard.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.algorithmintegration.AlgorithmConfiguration;
import io.dropwizard.Configuration;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by benjo on 5/16/16.
 */
public class TimelineAlgorithmConfiguration extends Configuration implements AlgorithmConfiguration {

    private final static  DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm");

    @JsonProperty("artificial_light_start_time")
    private String startTime = "21:30";

    @JsonProperty("artificial_light_stop_time")
    private String stopTime = "05:00";

    @Override
    public int getArtificalLightStartMinuteOfDay() {
        return DATE_TIME_FORMATTER.parseDateTime(startTime).getMinuteOfDay();
    }

    @Override
    public int getArtificalLightStopMinuteOfDay() {
        return DATE_TIME_FORMATTER.parseDateTime(stopTime).getMinuteOfDay();
    }
}

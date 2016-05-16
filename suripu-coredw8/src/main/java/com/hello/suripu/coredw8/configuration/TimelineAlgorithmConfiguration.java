package com.hello.suripu.coredw8.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.algorithmintegration.AlgorithmConfiguration;
import io.dropwizard.Configuration;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by benjo on 5/16/16.
 */
public class TimelineAlgorithmConfiguration extends Configuration implements AlgorithmConfiguration {

    private int artificalLightStartMinute;
    private int artificalLightStopMinute;

    public TimelineAlgorithmConfiguration(@JsonProperty("artificial_light_start_time") final String startTimeStr, @JsonProperty("artificial_light_stop_time") final String stopTimeStr) {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm");
        final DateTime startTime = formatter.parseDateTime(startTimeStr);
        final DateTime stopTime = formatter.parseDateTime(stopTimeStr);

        artificalLightStartMinute = startTime.getMinuteOfDay();
        artificalLightStopMinute = stopTime.getMinuteOfDay();
    }

    @Override
    public int getArtificalLightStartMinuteOfDay() {
        return artificalLightStartMinute;
    }

    @Override
    public int getArtificalLightStopMinuteOfDay() {
        return artificalLightStopMinute;
    }
}

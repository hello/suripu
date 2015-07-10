package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.Timeline;
import com.yammer.dropwizard.json.GuavaExtrasModule;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SleepMetricsTest {
    final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new GuavaModule())
            .registerModule(new GuavaExtrasModule())
            .registerModule(new JodaModule());


    public String getJSON(final String filePath) throws Exception {
        URL url = Resources.getResource(filePath);
        return Resources.toString(url, Charsets.UTF_8);
    }

    public com.hello.suripu.core.models.Timeline timelineV1WithStatistics() throws Exception {
        final String json = getJSON("fixtures/timeline/timeline-with-statistics.json");
        final List<com.hello.suripu.core.models.Timeline> timelines = objectMapper.readValue(json,
                new TypeReference<List<Timeline>>() {
                });
        return timelines.get(0);
    }

    public com.hello.suripu.core.models.Timeline timelineV1WithoutStatistics() throws Exception {
        final String json = getJSON("fixtures/timeline/timeline-without-statistics.json");
        final List<com.hello.suripu.core.models.Timeline> timelines = objectMapper.readValue(json,
                new TypeReference<List<Timeline>>() {});
        return timelines.get(0);
    }

    @Test
    public void withStatistics() throws Exception {
        final Timeline timelineV1 = timelineV1WithStatistics();

        Map<String, SleepMetrics> sleepMetrics = new HashMap<>();
        for (SleepMetrics metric : SleepMetrics.fromV1(timelineV1)) {
            sleepMetrics.put(metric.name, metric);
        }

        assertThat(sleepMetrics.size(), is(11));
        assertThat(sleepMetrics.keySet().containsAll(Lists.newArrayList("total_sleep", "sound_sleep", "time_to_sleep", "times_awake")), is(true));

        assertThat(sleepMetrics.get("total_sleep").value, is(Optional.of(330L)));
        assertThat(sleepMetrics.get("sound_sleep").value, is(Optional.of(106L)));
        assertThat(sleepMetrics.get("time_to_sleep").value, is(Optional.of(0L)));
        assertThat(sleepMetrics.get("times_awake").value, is(Optional.of(3L)));

        assertThat(sleepMetrics.get("fell_asleep").value, is(Optional.of(0L)));
        assertThat(sleepMetrics.get("woke_up").value, is(Optional.of(0L)));

        assertThat(sleepMetrics.get("temperature").value, is(Optional.<Long>absent()));
        assertThat(sleepMetrics.get("sound").value, is(Optional.<Long>absent()));
        assertThat(sleepMetrics.get("light").value, is(Optional.<Long>absent()));
        assertThat(sleepMetrics.get("humidity").value, is(Optional.<Long>absent()));
        assertThat(sleepMetrics.get("particulates").value, is(Optional.<Long>absent()));

        assertThat(sleepMetrics.get("particulates").condition, is(CurrentRoomState.State.Condition.ALERT));
        assertThat(sleepMetrics.get("humidity").condition, is(CurrentRoomState.State.Condition.IDEAL));
        assertThat(sleepMetrics.get("light").condition, is(CurrentRoomState.State.Condition.IDEAL));
        assertThat(sleepMetrics.get("temperature").condition, is(CurrentRoomState.State.Condition.IDEAL));
        assertThat(sleepMetrics.get("sound").condition, is(CurrentRoomState.State.Condition.IDEAL));
    }

    @Test
    public void withoutStatistics() throws Exception {
        final Timeline timelineV1 = timelineV1WithoutStatistics();

        Map<String, SleepMetrics> sleepMetrics = new HashMap<>();
        for (SleepMetrics metric : SleepMetrics.fromV1(timelineV1)) {
            sleepMetrics.put(metric.name, metric);
        }

        assertThat(sleepMetrics.size(), is(5));
        assertThat(sleepMetrics.keySet().containsAll(Lists.newArrayList("total_sleep", "sound_sleep", "time_to_sleep", "times_awake")), is(false));

        assertThat(sleepMetrics.get("temperature").value, is(Optional.<Long>absent()));
        assertThat(sleepMetrics.get("sound").value, is(Optional.<Long>absent()));
        assertThat(sleepMetrics.get("light").value, is(Optional.<Long>absent()));
        assertThat(sleepMetrics.get("humidity").value, is(Optional.<Long>absent()));
        assertThat(sleepMetrics.get("particulates").value, is(Optional.<Long>absent()));

        assertThat(sleepMetrics.get("particulates").condition, is(CurrentRoomState.State.Condition.ALERT));
        assertThat(sleepMetrics.get("humidity").condition, is(CurrentRoomState.State.Condition.IDEAL));
        assertThat(sleepMetrics.get("light").condition, is(CurrentRoomState.State.Condition.IDEAL));
        assertThat(sleepMetrics.get("temperature").condition, is(CurrentRoomState.State.Condition.IDEAL));
        assertThat(sleepMetrics.get("sound").condition, is(CurrentRoomState.State.Condition.IDEAL));
    }
}

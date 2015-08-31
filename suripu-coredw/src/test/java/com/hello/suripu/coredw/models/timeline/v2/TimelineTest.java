package com.hello.suripu.coredw.models.timeline.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.timeline.v2.ScoreCondition;
import com.hello.suripu.core.models.timeline.v2.Timeline;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.dropwizard.json.GuavaExtrasModule;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Test;

import java.net.URL;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class TimelineTest {
    final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new GuavaModule())
            .registerModule(new GuavaExtrasModule())
            .registerModule(new JodaModule());


    public String getJSON(final String filePath) throws Exception {
        URL url = Resources.getResource(filePath);
        return Resources.toString(url, Charsets.UTF_8);
    }

    public com.hello.suripu.core.models.Timeline timelineV1() throws Exception {
        final String json = getJSON("fixtures/timeline/timeline-with-statistics.json");
        final List<com.hello.suripu.core.models.Timeline> timelines = objectMapper.readValue(json,
                new TypeReference<List<com.hello.suripu.core.models.Timeline>>() {});
        return timelines.get(0);
    }


    @Test
    public void fromV1() throws Exception {
        final com.hello.suripu.core.models.Timeline timelineV1 = timelineV1();
        final Timeline converted = Timeline.fromV1(timelineV1, false);

        assertThat(converted.dateNight, is(timelineV1.date));
        assertThat(converted.message, is(timelineV1.message));

        assertThat(converted.score.isPresent(), is(true));
        assertThat(converted.score.get(), is(timelineV1.score));
        assertThat(converted.scoreCondition, CoreMatchers.is(ScoreCondition.WARNING));

        assertThat(converted.events, is(not(empty())));
        assertThat(converted.metrics, is(not(empty())));
    }

    @Test
    public void emptyStateSane() throws Exception {
        DateTime now = DateTime.now();
        final Timeline empty = Timeline.createEmpty(now, "Test");

        assertThat(empty.score, is(Optional.<Integer>absent()));
        assertThat(empty.scoreCondition, is(ScoreCondition.UNAVAILABLE));

        assertThat(empty.dateNight, is(DateTimeUtil.dateToYmdString(now)));
        assertThat(empty.message, is("Test"));

        assertThat(empty.events, is(empty()));
        assertThat(empty.metrics, is(empty()));
    }
}

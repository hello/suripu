package com.hello.suripu.workers;

import com.hello.suripu.workers.logs.SenseStructuredLogIndexer;
import java.util.Set;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jnorgan on 6/11/15.
 */
public class SenseStructuredLogIndexerTest {
    @Test
    public void testStructuredMessageDecode() {
        final String goodMessage = "{event_1:value1}{event_2:value2,event_3:3,event_4:true}";
        final String badMessage = "{ event_1:value1} {event_2:value2, event_3:3,event_4: true}";
        final String uglyMessage = "{ event_1:}value1}{event_2,:value2, :3,event_4:}";

        final Set<String> goodResults = SenseStructuredLogIndexer.decode(goodMessage);
        final Set<String> badResults = SenseStructuredLogIndexer.decode(badMessage);
        final Set<String> uglyResults = SenseStructuredLogIndexer.decode(uglyMessage);
        final Set<String> emptyResults = SenseStructuredLogIndexer.decode("");

        assertThat(goodResults.isEmpty(), is(false));
        assertThat(goodResults.size(), is(4));

        assertThat(badResults.isEmpty(), is(false));
        assertThat(badResults.size(), is(4));

        assertThat(uglyResults.isEmpty(), is(true));
        assertThat(emptyResults.isEmpty(), is(true));

    }
}

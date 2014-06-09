package com.hello.suripu.core.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.suripu.core.db.util.Compress;
import com.hello.suripu.core.models.Event;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Created by pangwu on 6/6/14.
 */
public class CompressionTest {

    @Test
    public void testGzipCompressionDecompression(){

        final Event.Type[] eventTypes = new Event.Type[]{Event.Type.MOTION, Event.Type.NOISE, Event.Type.LIGHT};
        final DateTime startOfTheDay = DateTime.now().withTimeAtStartOfDay();
        int numberOfMinutesPerDay = 24 * 60;
        final ArrayList<Event> allEventList = new ArrayList<Event>();
        for(final Event.Type type:eventTypes) {

            for (int i = 0; i < numberOfMinutesPerDay; i++) {
                final DateTime eventStartTime = startOfTheDay.plusMinutes(i);
                final Event event = new Event(type,
                        (eventStartTime.getMillis()),
                        (eventStartTime.plusMinutes(1).getMillis()),
                        DateTimeZone.getDefault().getOffset(eventStartTime));

                allEventList.add(event);
            }



        }

        final ObjectMapper mapper = new ObjectMapper();

        try {
            // Make sure we put some large enough thing so the compressed data is larger than the uncompressed one.
            final String expected = mapper.writeValueAsString(allEventList);
            final byte[] uncompress = expected.getBytes("UTF-8");
            final byte[] compressed = Compress.gzipCompress(uncompress);
            assertThat(compressed.length, lessThan(uncompress.length));

            final byte[] decompressed = Compress.gzipDecompress(compressed);
            final String actual = new String(decompressed, "UTF-8");
            assertThat(actual, equalTo(expected));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    @Test
    public void testBZip2CompressionDecompression(){

        final Event.Type[] eventTypes = new Event.Type[]{Event.Type.MOTION, Event.Type.NOISE, Event.Type.LIGHT};
        final DateTime startOfTheDay = DateTime.now().withTimeAtStartOfDay();
        int numberOfMinutesPerDay = 24 * 60;
        final ArrayList<Event> allEventList = new ArrayList<Event>();

        for(final Event.Type type:eventTypes) {

            for (int i = 0; i < numberOfMinutesPerDay; i++) {
                final DateTime eventStartTime = startOfTheDay.plusMinutes(i);
                final Event event = new Event(type,
                        (eventStartTime.getMillis()),
                        (eventStartTime.plusMinutes(1).getMillis()),
                        DateTimeZone.getDefault().getOffset(eventStartTime));

                allEventList.add(event);
            }



        }

        final ObjectMapper mapper = new ObjectMapper();

        try {
            // Make sure we put some large enough thing so the compressed data is larger than the uncompressed one.
            final String expected = mapper.writeValueAsString(allEventList);
            final byte[] uncompress = expected.getBytes("UTF-8");
            final byte[] compressed = Compress.bzip2Compress(uncompress);
            assertThat(compressed.length, lessThan(uncompress.length));

            final byte[] decompressed = Compress.bzip2Decompress(compressed);
            final String actual = new String(decompressed, "UTF-8");
            assertThat(actual, equalTo(expected));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}

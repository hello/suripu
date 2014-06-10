package com.hello.suripu.core.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.suripu.core.db.util.Compression;
import com.hello.suripu.core.models.Event;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Created by pangwu on 6/6/14.
 */
public class CompressionTest {

    private byte[] rawDataInBytes = null;


    @Before
    public void setUp(){
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
            this.rawDataInBytes = expected.getBytes("UTF-8");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testGzipCompressionDecompression(){



        try {
            // Make sure we put some large enough thing so the compressed data is larger than the uncompressed one.

            final byte[] compressed = Compression.gzipCompress(this.rawDataInBytes);
            assertThat(compressed.length, lessThan(this.rawDataInBytes.length));

            final byte[] decompressed = Compression.gzipDecompress(compressed);
            final String actual = new String(decompressed, "UTF-8");
            assertThat(actual, equalTo(new String(this.rawDataInBytes, "UTF-8")));
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


        try {
            // Make sure we put some large enough thing so the compressed data is larger than the uncompressed one.

            final byte[] compressed = Compression.bzip2Compress(this.rawDataInBytes);
            assertThat(compressed.length, lessThan(this.rawDataInBytes.length));

            final byte[] decompressed = Compression.bzip2Decompress(compressed);
            final String actual = new String(decompressed, "UTF-8");
            assertThat(actual, equalTo(new String(this.rawDataInBytes, "UTF-8")));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    @Test
    public void testCompressionTime(){
        int testRound = 100;

        long startTimestamp = DateTime.now().getMillis();

        try {
            for (int i = 0; i < testRound; i++) {
                Compression.bzip2Compress(this.rawDataInBytes);
            }

            long bzip2CompressionTime = DateTime.now().getMillis() - startTimestamp;

            startTimestamp = DateTime.now().getMillis();
            for(int i = 0; i < testRound; i++){
                Compression.gzipCompress(this.rawDataInBytes);
            }

            long gzipCompressionTime = DateTime.now().getMillis() - startTimestamp;

            assertThat(gzipCompressionTime, lessThan(bzip2CompressionTime));
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}

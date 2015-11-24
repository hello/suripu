package com.hello.suripu.core.db;

import com.google.common.io.Resources;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Created by pangwu on 9/18/14.
 */
public class TimeZoneChangeTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimeZoneChangeTest.class);

    @Test
    public void testDayLightSavingTimeZoneName(){
        final DateTimeZone PST = DateTimeZone.forID("America/Los_Angeles");
        //final DateTimeZone PST = DateTimeZone.forTimeZone(TimeZone.getTimeZone("PDT"));
        final DateTime dayLightSaving = new DateTime(2014, 3, 8, 0, 0, 0, PST);
        final DateTime normalTime = new DateTime(2014, 3, 11, 0, 0, 0, PST);

        LOGGER.debug("{}", dayLightSaving.getZone().getOffset(dayLightSaving));
        LOGGER.debug("{}", normalTime.getZone().getOffset(normalTime));

        LOGGER.debug("{}", dayLightSaving.getZone().getName(dayLightSaving.getMillis()));
        LOGGER.debug("{}", normalTime.getZone().getName(normalTime.getMillis()));

        assertThat(normalTime.getZone().getOffset(normalTime) == dayLightSaving.getZone().getOffset(dayLightSaving), is(false));
        assertThat(normalTime.getZone().getID().equals(dayLightSaving.getZone().getID()), is(true));
    }

    @Test
    public void testTimeZOneName(){
        final DateTimeZone PST = DateTimeZone.forID("America/Los_Angeles");
        final DateTime currentTime = new DateTime(DateTime.now().getMillis(), PST);

        final DateTime currentTime2 = new DateTime(DateTime.now().getMillis(), DateTimeZone.forOffsetMillis(DateTime.now().getZone().getOffset(DateTime.now())));
        assertThat(currentTime2.getZone().getID(), not("America/Los_Angeles"));
    }


    //@Test(expected = IllegalArgumentException.class)
    public void testiOSTimeZoneIDToJodaTimeTimeZoneId(){
        final File resourceFile = new File(Resources.getResource("ios_timezone_ids.txt").getFile());


        final Set<String> jodaTimeZoneNames = DateTimeZone.getAvailableIDs();

        try {
            final BufferedReader reader = new BufferedReader(new FileReader(resourceFile));
            String line = reader.readLine();
            while (line != null) {
                if(line.endsWith(",")){
                    line = line.substring(0, line.length() - 1);
                }

                if(!jodaTimeZoneNames.contains(line)){
                    LOGGER.trace("{}", line);
                }
                final DateTimeZone zone = DateTimeZone.forID(line);

                line = reader.readLine();
            }

            reader.close();
        }catch (IOException ex){

        }



    }
}

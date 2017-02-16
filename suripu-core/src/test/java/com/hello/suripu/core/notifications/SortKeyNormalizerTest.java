package com.hello.suripu.core.notifications;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SortKeyNormalizerTest {

    static class Input {
        public final String title;
        public final DateTime eventHappensOn;
        public final DateTime eventShouldHappenOn;

        Input(String title, DateTime eventHappensOn, DateTime eventShouldHappenOn) {
            this.title = title;
            this.eventHappensOn = eventHappensOn;
            this.eventShouldHappenOn = eventShouldHappenOn;
        }
    }
    @Test
    public void testWeekSortKey() {
        final DateTimeZone timeZone = DateTimeZone.forID("America/Los_Angeles");

        final List<Input> inputList = Lists.newArrayList(
                new Input(
                        "Monday -> Monday",
                        new DateTime(2017,2,13, 18,29,0,timeZone),
                        new DateTime(2017,2,13,0,0,0, timeZone)
                ),
                new Input(
                        "Tuesday -> Monday",
                        new DateTime(2017,2,14, 18,32,0,timeZone),
                        new DateTime(2017,2,13, 18,29,0,timeZone)
                ),
                new Input(
                        "Thursday -> Monday prev month",
                        new DateTime(2017,6,1,18,36,0, timeZone),
                        new DateTime(2017,5,29,0,0,0, timeZone)
                ),
                new Input(
                        "Friday -> Monday prev year",
                        new DateTime(2016,1,1,7,0,0, timeZone),
                        new DateTime(2015,12,28, 0,0,0, timeZone)
                ),

                new Input(
                        "In Paris",
                        new DateTime(2017,2,13, 18,29,0, DateTimeZone.forID("Europe/Paris")),
                        new DateTime(2017,2,13,0,0,0, DateTimeZone.forID("Europe/Paris"))
                )

        );


        for(final Input input : inputList) {
            final DateTime result = SortKeyNormalizer.byWeek(input.eventHappensOn, input.eventHappensOn.getZone());
            assertEquals(input.title, input.eventShouldHappenOn.getYear(), result.getYear());
            assertEquals(input.title, 0, result.getHourOfDay());
            assertEquals(input.title, input.eventShouldHappenOn.getDayOfYear(), result.getDayOfYear());
            assertEquals(input.title, input.eventShouldHappenOn.getDayOfMonth(), result.getDayOfMonth());
        }
    }
}

package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;



/**
 * Created by jarredheinrich on 10/7/16.
 */

public class TimelineUtilsTest {

    final private TimelineUtils timelineUtils = new TimelineUtils();

    private List<TrackerMotion> loadTrackerMotionFromCSV(final String resource){
        final URL fixtureCSVFile = Resources.getResource(resource);
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final TrackerMotion trackerMotion = new TrackerMotion(0L, 0L, 0L, Long.valueOf(columns[0]), Integer.valueOf(columns[1]), Integer.valueOf(columns[2]), 0L, 0L,0L);
                //if(trackerMotion.value > 0){
                trackerMotions.add(trackerMotion);
                //}
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        return trackerMotions;
    }

    @Test
    public void testComputeStats() {
        //false positive night - motion at start and end of night
        final List<TrackerMotion> trackerMotions =  loadTrackerMotionFromCSV("fixtures/algorithm/millionaires_challenge_2015_02_20_raw.csv");
        final List<MotionEvent> motionEvents = timelineUtils.generateMotionEvents(trackerMotions);
        final long sleepTime = trackerMotions.get(0).timestamp;
        final long wakeTime = trackerMotions.get(0).timestamp + 24000000L;
        final TimeZoneHistory timeZoneHistory1 = new TimeZoneHistory(1428408400000L, 3600000, "America/Los_Angeles");
        final List<TimeZoneHistory> timeZoneHistoryList = new ArrayList<>();
        timeZoneHistoryList.add(timeZoneHistory1);
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryList);

        final Optional<Event> sleep = Optional.of(Event.createFromType(Event.Type.SLEEP, sleepTime, sleepTime, -25200000, Optional.of(English.IN_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));
        final Optional<Event> wake = Optional.of(Event.createFromType(Event.Type.WAKE_UP, wakeTime, wakeTime, -25200000, Optional.of(English.OUT_OF_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));

        final List<Event> mainEvents = new ArrayList<Event>();

        mainEvents.add(sleep.get());
        mainEvents.add(wake.get());


        final Map<Long, Event> timelineEvents = TimelineRefactored.populateTimeline(motionEvents, timeZoneOffsetMap);
        for (final Event event : mainEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }
        final List<Event> eventsWithSleepEvents = TimelineRefactored.mergeEvents(timelineEvents);
        final List<Event> smoothedEvents = timelineUtils.smoothEvents(eventsWithSleepEvents);

        List<Event> cleanedUpEvents;
        cleanedUpEvents = timelineUtils.removeMotionEventsOutsideBedPeriod(smoothedEvents, sleep, wake);
        final List<Event> greyEvents = timelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents, sleep, wake);
        final List<Event> nonSignificantFilteredEvents = timelineUtils.removeEventBeforeSignificant(greyEvents);
        final List<SleepSegment> sleepSegments = timelineUtils.eventsToSegments(nonSignificantFilteredEvents);
        final SleepStats sleepStats = TimelineUtils.computeStats(sleepSegments, trackerMotions, 70, true, true);
        final Integer uninterruptedDuration = 380;
        assertThat(sleepStats.uninterruptedSleepDurationInMinutes, is(uninterruptedDuration));
    }

    @Test
    public void testGetPrevNightMainEventTimes(){
        final long accountId = 0L;

        final DateTime startTime = new DateTime(2017,2,1,0,0, DateTimeZone.UTC);

        final DateTime createdAt = startTime.plusDays(3);
        final List<MainEventTimes> testMainEventTimesList = Lists.newArrayList();
        for(int i = 0; i < 2; i ++){
            final MainEventTimes testMainEventTimes = MainEventTimes.createMainEventTimesEmpty(accountId, SleepPeriod.createSleepPeriod(SleepPeriod.Period.fromInteger(i), startTime), createdAt.getMillis(), 0, AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);
            testMainEventTimesList.add(testMainEventTimes);
        }
        final MainEventTimes mainEventTimesNight = MainEventTimes.createMainEventTimes(accountId, startTime.withHourOfDay(20).getMillis(), 0, startTime.withHourOfDay(21).getMillis(), 0, startTime.withHourOfDay(22).getMillis(), 0, startTime.withHourOfDay(23).getMillis(), 0, createdAt.getMillis(), 0 , AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);
        testMainEventTimesList.add(mainEventTimesNight);

        final MainEventTimes prevNight =  TimelineUtils.getPrevNightMainEventTimes(0L, testMainEventTimesList, startTime.plusDays(1));
        assert(prevNight.eventTimeMap.get(Event.Type.OUT_OF_BED).time.equals(mainEventTimesNight.eventTimeMap.get(Event.Type.OUT_OF_BED).time));
    }

    @Test
    public void testGetTargetDate(){
        final TimeZoneHistory timeZoneHistory1 = new TimeZoneHistory(1428408400000L, 3600000, "America/Los_Angeles");
        final List<TimeZoneHistory> timeZoneHistoryList = new ArrayList<>();
        timeZoneHistoryList.add(timeZoneHistory1);
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryList);

        //still query date and not daysleeper
        boolean isDaySleeper = false;
        DateTime queryDate = new DateTime(2017, 02, 13, 00,00,00, DateTimeZone.UTC);
        final Optional<Integer> queryHourOptional = Optional.of(2);
        DateTime currentTimeLocal = new DateTime(2017, 02, 13, 13,00,00, DateTimeZone.forID("America/Los_Angeles"));
        DateTime targetDate = TimelineUtils.getTargetDate(isDaySleeper, queryDate, currentTimeLocal, queryHourOptional, timeZoneOffsetMap);
        assert(targetDate.getMillis() == queryDate.minusDays(1).getMillis());

        //still query date but daysleeper
        isDaySleeper = true;
        targetDate = TimelineUtils.getTargetDate(isDaySleeper, queryDate, currentTimeLocal, queryHourOptional, timeZoneOffsetMap);
        assert(targetDate.getMillis() == queryDate.getMillis());

        //after query date and not day sleeper
        isDaySleeper = false;
        currentTimeLocal = new DateTime(2017, 02, 14, 00,00,00, DateTimeZone.forID("America/Los_Angeles"));
        targetDate = TimelineUtils.getTargetDate(isDaySleeper, queryDate, currentTimeLocal, queryHourOptional, timeZoneOffsetMap);
        assert(targetDate.getMillis() == queryDate.getMillis());

        isDaySleeper = false;
        currentTimeLocal = new DateTime(2017, 02, 13, 00,00,00, DateTimeZone.forID("America/Los_Angeles"));
        targetDate = TimelineUtils.getTargetDate(isDaySleeper, queryDate, currentTimeLocal, Optional.absent(), timeZoneOffsetMap);
        assert(targetDate.getMillis() == queryDate.getMillis());
    }


}
package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.util.TimeZoneOffsetMap;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jarredheinrich on 3/13/17.
 */
public class SleepPeriodResults {

    public final MainEventTimes mainEventTimes;
    public final Optional<Results> resultsOptional;
    public final TimelineLog timelineLog;
    public final DataCompleteness dataCompleteness;
    public final boolean processed;
    SleepPeriodResults(final MainEventTimes mainEventTimes, final Optional<Results> resultsOptional, final TimelineLog timelineLog, final DataCompleteness dataCompleteness, final boolean isValid){
        this.mainEventTimes= mainEventTimes;
        this.resultsOptional = resultsOptional;
        this.timelineLog = timelineLog;
        this.dataCompleteness = dataCompleteness;
        this.processed = isValid;

    }
    public static class Results {
        public final OneDaysSensorData sensorData;
        public final TimeZoneOffsetMap timeZoneOffsetMap;
        public final Timeline timeline;
        public final SleepScore sleepScore;
        public final SleepStats sleepStats;

        private Results(final Timeline timeline, final SleepScore sleepScore, final SleepStats sleepStats,  final OneDaysSensorData oneDaysSensorData, final TimeZoneOffsetMap timeZoneOffsetMap) {
            this.timeline = timeline;
            this.sleepScore = sleepScore;
            this.sleepStats = sleepStats;
            this.sensorData = oneDaysSensorData;
            this.timeZoneOffsetMap = timeZoneOffsetMap;
        }
    }
    public static SleepPeriodResults create(final MainEventTimes mainEventTimes, final Timeline timeline, final SleepScore sleepScore,final SleepStats sleepStats, final OneDaysSensorData sensorData, final TimeZoneOffsetMap timeZoneOffsetMap, final TimelineLog timelineLog, final DataCompleteness dataCompleteness, final boolean processed) {
        final Results results = new Results(timeline, sleepScore,sleepStats, sensorData, timeZoneOffsetMap);
        return new SleepPeriodResults(mainEventTimes, Optional.of(results), timelineLog, dataCompleteness, processed);
    }
    public static SleepPeriodResults create(final MainEventTimes mainEventTimes, final Optional<Results> resultsOptional, final TimelineLog timelineLog, final DataCompleteness dataCompleteness,  final boolean processed) {
        return new SleepPeriodResults(mainEventTimes, resultsOptional, timelineLog, dataCompleteness, processed);
    }
    public static SleepPeriodResults createEmpty(final long accountId, final SleepPeriod sleepPeriod, final TimelineLog timelineLog, final DataCompleteness dataCompleteness,  final boolean processed) {
        return new SleepPeriodResults(MainEventTimes.createMainEventTimesEmpty(accountId, sleepPeriod, DateTime.now(DateTimeZone.UTC).getMillis(), 0), Optional.absent(), timelineLog, dataCompleteness,  processed);
    }
}

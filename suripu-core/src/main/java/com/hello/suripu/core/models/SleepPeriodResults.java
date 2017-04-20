package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.util.TimeZoneOffsetMap;

/**
 * Created by jarredheinrich on 3/13/17.
 */
public class SleepPeriodResults {

    public final MainEventTimes mainEventTimes;
    public final Optional<Results> resultsOptional;
    public final Optional<Data> dataOptional;
    public final TimelineLog timelineLog;
    public final DataCompleteness dataCompleteness;
    public final boolean processed;
    SleepPeriodResults(final MainEventTimes mainEventTimes, final Optional<Results> resultsOptional, final Optional<Data> dataOptional, final TimelineLog timelineLog, final DataCompleteness dataCompleteness, final boolean isValid){
        this.mainEventTimes= mainEventTimes;
        this.resultsOptional = resultsOptional;
        this.dataOptional = dataOptional;
        this.timelineLog = timelineLog;
        this.dataCompleteness = dataCompleteness;
        this.processed = isValid;

    }
    public static class Results {
        public final Timeline timeline;
        public final SleepScore sleepScore;
        public final SleepStats sleepStats;

        private Results(final Timeline timeline, final SleepScore sleepScore, final SleepStats sleepStats) {
            this.timeline = timeline;
            this.sleepScore = sleepScore;
            this.sleepStats = sleepStats;
        }
    }

    public static class Data {
        public final OneDaysSensorData sensorData;
        public final TimeZoneOffsetMap timeZoneOffsetMap;

        private Data(final OneDaysSensorData sensorData, final TimeZoneOffsetMap timeZoneOffsetMap){
            this.sensorData = sensorData;
            this.timeZoneOffsetMap = timeZoneOffsetMap;
        }
    }

    public static SleepPeriodResults create(final MainEventTimes mainEventTimes, final Timeline timeline, final SleepScore sleepScore,final SleepStats sleepStats, final OneDaysSensorData sensorData, final TimeZoneOffsetMap timeZoneOffsetMap, final TimelineLog timelineLog, final DataCompleteness dataCompleteness, final boolean processed) {
        final Results results = new Results(timeline, sleepScore,sleepStats);
        final Data data = new Data(sensorData, timeZoneOffsetMap);
        return new SleepPeriodResults(mainEventTimes, Optional.of(results),Optional.of(data), timelineLog, dataCompleteness, processed);
    }
    public static SleepPeriodResults create(final MainEventTimes mainEventTimes, final Optional<Results> resultsOptional,final Optional<Data> dataOptional, final TimelineLog timelineLog, final DataCompleteness dataCompleteness,  final boolean processed) {
        return new SleepPeriodResults(mainEventTimes, resultsOptional, dataOptional, timelineLog, dataCompleteness, processed);
    }
    public static SleepPeriodResults createEmpty(final long accountId, final MainEventTimes mainEventTimes, final TimelineLog timelineLog, final DataCompleteness dataCompleteness,  final boolean processed) {
        return new SleepPeriodResults(mainEventTimes, Optional.absent(),Optional.absent(), timelineLog, dataCompleteness,  processed);
    }
}

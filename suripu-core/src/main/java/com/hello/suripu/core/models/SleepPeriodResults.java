package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.util.TimeZoneOffsetMap;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;

/**
 * Created by jarredheinrich on 3/13/17.
 */
public class SleepPeriodResults {
    public final SleepPeriod sleepPeriod;
    public final Optional<Results> resultsOptional;
    public final TimelineLog timelineLog;
    public final DataCompleteness dataCompleteness;
    public final boolean isNewResult;
    public final boolean isValid;
    SleepPeriodResults(final SleepPeriod sleepPeriod, final Optional<Results> resultsOptional, final TimelineLog timelineLog, final DataCompleteness dataCompleteness, final boolean isNewResult, final boolean isValid){
        this.sleepPeriod = sleepPeriod;
        this.resultsOptional = resultsOptional;
        this.timelineLog = timelineLog;
        this.dataCompleteness = dataCompleteness;
        this.isNewResult = isNewResult;
        this.isValid = isValid;

    }
    public static class Results {
        public final MainEventTimes mainEventTimes;
        public final OneDaysSensorData sensorData;
        public final TimeZoneOffsetMap timeZoneOffsetMap;
        public final Timeline timeline;
        public final SleepScore sleepScore;

        private Results(final MainEventTimes mainEventTimes, final Timeline timeline, final SleepScore sleepScore, final OneDaysSensorData oneDaysSensorData, final TimeZoneOffsetMap timeZoneOffsetMap) {
            this.mainEventTimes = mainEventTimes;
            this.timeline = timeline;
            this.sleepScore = sleepScore;
            this.sensorData = oneDaysSensorData;
            this.timeZoneOffsetMap = timeZoneOffsetMap;
        }
    }
    public static SleepPeriodResults create(final MainEventTimes mainEventTimes, final Timeline timeline, final SleepScore sleepScore, final OneDaysSensorData sensorData, final TimeZoneOffsetMap timeZoneOffsetMap, final TimelineLog timelineLog, final DataCompleteness dataCompleteness, final boolean isNewResult, final boolean isValid) {
        final Results results = new Results(mainEventTimes, timeline, sleepScore, sensorData, timeZoneOffsetMap);
        return new SleepPeriodResults(mainEventTimes.sleepPeriod, Optional.of(results), timelineLog, dataCompleteness, isNewResult, isValid);
    }
    public static SleepPeriodResults create(final SleepPeriod sleepPeriod, final Optional<Results> resultsOptional, final TimelineLog timelineLog, final DataCompleteness dataCompleteness, final boolean isNewResult, final boolean isValid) {
        return new SleepPeriodResults(sleepPeriod, resultsOptional, timelineLog, dataCompleteness, isNewResult, isValid);
    }
    public static SleepPeriodResults createEmpty(final SleepPeriod sleepPeriod, final TimelineLog timelineLog, final DataCompleteness dataCompleteness, final boolean isNewResult, final boolean isValid) {
        return new SleepPeriodResults(sleepPeriod, Optional.absent(), timelineLog, dataCompleteness, isNewResult, isValid);
    }
}

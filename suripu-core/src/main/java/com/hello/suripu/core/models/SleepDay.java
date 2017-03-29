package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 * Created by jarredheinrich on 3/14/17.
 */

//todo check if maineventtimes created date is valid
public class SleepDay {
    final DateTime targetDate;
    SleepPeriodResults morningResults;
    SleepPeriodResults afternoonResults;
    SleepPeriodResults nightResults;
    private SleepDay (final DateTime targetDate, final SleepPeriodResults morningResults, final SleepPeriodResults afternoonResults, final SleepPeriodResults nightResults){
        this.targetDate = targetDate;
        this.morningResults = morningResults;
        this.afternoonResults = afternoonResults;
        this.nightResults = nightResults;
    }

    public static SleepDay createSleepDay(final DateTime targetDate, final SleepPeriodResults morningResults, final SleepPeriodResults afternoonResults, final SleepPeriodResults nightResults){
        return new SleepDay(targetDate, morningResults, afternoonResults, nightResults);
    }

    public static SleepDay createSleepDay(final long accountId, final DateTime targetDate, List<MainEventTimes> generatedMainEventTimes){
        MainEventTimes morningEvents = MainEventTimes.createMainEventTimesEmpty(accountId, SleepPeriod.morning(targetDate), 0,0);
        MainEventTimes afternoonEvents = MainEventTimes.createMainEventTimesEmpty(accountId, SleepPeriod.afternoon(targetDate), 0,0);
        MainEventTimes nightEvents = MainEventTimes.createMainEventTimesEmpty(accountId, SleepPeriod.night(targetDate), 0,0);

        boolean morningProcessed = false;
        boolean afternoonProcessed = false;
        boolean nightProcessed = false;

        DataCompleteness morningDataCompleteness = DataCompleteness.NO_DATA;
        DataCompleteness afternoonDataCompleteness = DataCompleteness.NO_DATA;
        DataCompleteness nightDataCompleteness = DataCompleteness.NO_DATA;

        for(final MainEventTimes mainEventTimes : generatedMainEventTimes){
            if(mainEventTimes.sleepPeriod.targetDate.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().getMillis() != targetDate.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().withTimeAtStartOfDay().getMillis() ){
                continue;
            }
            if(mainEventTimes.sleepPeriod.period == SleepPeriod.Period.MORNING){
                morningEvents = mainEventTimes;
                morningProcessed = true;
                morningDataCompleteness = DataCompleteness.ENOUGH_DATA;
            }
            if(mainEventTimes.sleepPeriod.period == SleepPeriod.Period.AFTERNOON){
                afternoonEvents = mainEventTimes;
                afternoonProcessed = true;
                afternoonDataCompleteness = DataCompleteness.ENOUGH_DATA;
            }
            if(mainEventTimes.sleepPeriod.period == SleepPeriod.Period.NIGHT){
                nightEvents = mainEventTimes;
                nightProcessed = true;
                nightDataCompleteness = DataCompleteness.ENOUGH_DATA;
            }
        }
        // assumes data is complete and timelines are valid for periods with a prev. generated mainEventTImes;
        final SleepPeriodResults morningResults = SleepPeriodResults.create(morningEvents, Optional.absent(), new TimelineLog(accountId, targetDate.getMillis(), DateTime.now(DateTimeZone.UTC).getMillis()),morningDataCompleteness, morningProcessed);
        final SleepPeriodResults afternoonResults = SleepPeriodResults.create(afternoonEvents, Optional.absent(), new TimelineLog(accountId, targetDate.getMillis(), DateTime.now(DateTimeZone.UTC).getMillis()), afternoonDataCompleteness, afternoonProcessed);
        final SleepPeriodResults nightResults = SleepPeriodResults.create(nightEvents, Optional.absent(), new TimelineLog(accountId, targetDate.getMillis(), DateTime.now(DateTimeZone.UTC).getMillis()), nightDataCompleteness, nightProcessed);

        return new SleepDay(targetDate, morningResults, afternoonResults, nightResults);

    }

    public void updateSleepPeriod(final SleepPeriodResults updatedSleepPeriodResults){
        if(updatedSleepPeriodResults.mainEventTimes.sleepPeriod.period == SleepPeriod.Period.MORNING){
           this.morningResults = updatedSleepPeriodResults;
        }
        if(updatedSleepPeriodResults.mainEventTimes.sleepPeriod.period == SleepPeriod.Period.AFTERNOON){
            this.afternoonResults = updatedSleepPeriodResults;
        }
        if(updatedSleepPeriodResults.mainEventTimes.sleepPeriod.period == SleepPeriod.Period.AFTERNOON){
            this.afternoonResults = updatedSleepPeriodResults;
        }
        if(updatedSleepPeriodResults.mainEventTimes.sleepPeriod.period == SleepPeriod.Period.NIGHT){
            this.nightResults = updatedSleepPeriodResults;
        }
    }

    public SleepPeriodResults getSleepPeriod(final SleepPeriod.Period period){
        if (period == SleepPeriod.Period.MORNING){
            return this.morningResults;
        }
        if (period == SleepPeriod.Period.AFTERNOON){
            return this.afternoonResults;
        }
        return this.nightResults;
    }

    public Optional<Long> getPreviousOutOfBedTime(final SleepPeriod.Period period, final MainEventTimes prevNightMainEventTimes) {
        //checks if sleepPeriodsMainEventTimes maps contain previous period, and that period has valid main event times
        if ( period == SleepPeriod.Period.MORNING) {
            if (prevNightMainEventTimes.hasValidEventTimes()) {
                return Optional.of(prevNightMainEventTimes.eventTimeMap.get(Event.Type.OUT_OF_BED).time);
            } else {
                return Optional.absent();
            }
        }
        if (period == SleepPeriod.Period.AFTERNOON) {
            if (this.morningResults.processed && this.morningResults.mainEventTimes.hasValidEventTimes()) {
                return Optional.of(this.morningResults.mainEventTimes.eventTimeMap.get(Event.Type.OUT_OF_BED).time);
            } else {
                return Optional.absent();
            }
        }
        if (period == SleepPeriod.Period.NIGHT) {
            if (this.afternoonResults.processed && this.afternoonResults.mainEventTimes.hasValidEventTimes()) {
                return Optional.of(this.afternoonResults.mainEventTimes.eventTimeMap.get(Event.Type.OUT_OF_BED).time);
            } else {
                return Optional.absent();
            }
        }
        return Optional.absent();

    }


}

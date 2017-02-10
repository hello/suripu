package com.hello.suripu.core.models;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jarredheinrich on 2/2/17.
 */
public class SleepPeriod {
    public final SleepPeriod.Period period;
    public final ImmutableMap<Boundary, Integer> hoursOffset;
    public final DateTime targetDate;

    public SleepPeriod(final SleepPeriod.Period sleepPeriod, final DateTime targetDate) {
        this.period = sleepPeriod;
        this.targetDate = targetDate.withTimeAtStartOfDay();

        if (sleepPeriod == SleepPeriod.Period.MORNING) {
            this.hoursOffset = ImmutableMap.<Boundary, Integer>builder()
                    .put(Boundary.START,4)
                    .put(Boundary.END_IN_BED, 12)
                    .put(Boundary.END_DATA, 20)
                    .build();
        } else if (sleepPeriod == SleepPeriod.Period.AFTERNOON_EVENING) {
            this.hoursOffset = ImmutableMap.<Boundary, Integer>builder()
                    .put(Boundary.START,12)
                    .put(Boundary.END_IN_BED, 20)
                    .put(Boundary.END_DATA, 28)
                    .build();
        } else{
            this.hoursOffset = ImmutableMap.<Boundary, Integer>builder()
                    .put(Boundary.START,20)
                    .put(Boundary.END_IN_BED, 28)
                    .put(Boundary.END_DATA, 36)
                    .build();
        }
    }

    public static SleepPeriod createSleepPeriod(final DateTime inBedTime){
        final Integer inBedHour = inBedTime.getHourOfDay();
        if (inBedHour >= 4 && inBedHour < 12){
            final DateTime targetDate = inBedTime.withTimeAtStartOfDay();
            return SleepPeriod.morning(targetDate);
        } else if (inBedHour >= 12 && inBedHour < 20){
            final DateTime targetDate = inBedTime.withTimeAtStartOfDay();
            return SleepPeriod.afternoonEvening(targetDate);
        } else {
            final DateTime targetDate;
            if (inBedHour < 4){
                targetDate = inBedTime.withTimeAtStartOfDay().minusDays(1);
            }else{
                targetDate = inBedTime.withTimeAtStartOfDay();
            }
            return SleepPeriod.night(targetDate);
        }
    }

    public static SleepPeriod createSleepPeriod(final Period period, final DateTime targetDate){
        if (period == Period.MORNING){
            return SleepPeriod.morning(targetDate);
        }
        if(period == period.AFTERNOON_EVENING){
            return SleepPeriod.afternoonEvening(targetDate);
        }
        return SleepPeriod.night(targetDate);

    }

    public static SleepPeriod night(final DateTime targetDate){
        return new SleepPeriod(Period.NIGHT, targetDate);
    }

    public static  SleepPeriod afternoonEvening(final DateTime targetDate){
        return new SleepPeriod(Period.AFTERNOON_EVENING, targetDate);
    }

    public static  SleepPeriod morning(final DateTime targetDate){
        return new SleepPeriod(Period.MORNING, targetDate);
    }


    public enum Period {
        NONE(-1) {
            public String toString() {
                return "";
            }
        },
        MORNING(0),
        AFTERNOON_EVENING(1),
        NIGHT(2);
        private int value;

        private Period(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static SleepPeriod.Period fromInteger(int value) {
            for (final SleepPeriod.Period period : SleepPeriod.Period.values()) {
                if (period.value == value) {
                    return period;
                }
            }
            return NONE;
        }


    }

    public enum Boundary {
        NONE(-1) {
            public String toString() {
                return "";
            }
        },
        START(0),
        END_IN_BED(1),
        END_DATA(2);
        private int value;

        private Boundary(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

    }

    public DateTime getSleepPeriodTime(final Boundary boundary){
        switch(period) {
            case MORNING:
                return targetDate.withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary));
            case AFTERNOON_EVENING:
                return targetDate.withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary));
            default:
                return targetDate.withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary));
        }
    }




    public boolean sleepEventInSleepPeriod(final DateTime inBedTime){
        final SleepPeriod eventSleepPeriod = createSleepPeriod(inBedTime);
        if (this.period == eventSleepPeriod.period){
            return true;
        }
        return false;
    }

    public static List<SleepPeriod.Period> getAll(){
        final List<SleepPeriod.Period> periods = new ArrayList<>();
        periods.add(Period.MORNING);
        periods.add(Period.AFTERNOON_EVENING);
        periods.add(Period.NIGHT);
        return periods;
    }

}

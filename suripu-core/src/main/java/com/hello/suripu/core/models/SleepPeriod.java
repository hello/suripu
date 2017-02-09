package com.hello.suripu.core.models;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jarredheinrich on 2/2/17.
 */
public class SleepPeriod {
    public final SleepPeriod.Period PERIOD;
    public final Map<Boundary, Integer> HOURS_OFFSET;
    public final DateTime TARGET_DATE;

    public SleepPeriod(final SleepPeriod.Period sleepPeriod, final DateTime targetDate) {
        this.PERIOD = sleepPeriod;
        this.TARGET_DATE = targetDate.withTimeAtStartOfDay();
        this.HOURS_OFFSET = new HashMap<>();
        if (sleepPeriod == SleepPeriod.Period.MORNING) {
            this.HOURS_OFFSET.put(Boundary.START,4);
            this.HOURS_OFFSET.put(Boundary.END_IN_BED, 12);
            this.HOURS_OFFSET.put(Boundary.END_DATA, 20);

        } else if (sleepPeriod == SleepPeriod.Period.AFTERNOON_EVENING) {
            this.HOURS_OFFSET.put(Boundary.START,12);
            this.HOURS_OFFSET.put(Boundary.END_IN_BED, 20);
            this.HOURS_OFFSET.put(Boundary.END_DATA, 28);
        } else{
            this.HOURS_OFFSET.put(Boundary.START, 20);
            this.HOURS_OFFSET.put(Boundary.END_IN_BED, 28);
            this.HOURS_OFFSET.put(Boundary.END_DATA, 36);
        }
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
        switch(PERIOD) {
            case MORNING:
                return TARGET_DATE.withTimeAtStartOfDay().plusHours(this.HOURS_OFFSET.get(boundary));
            case AFTERNOON_EVENING:
                return TARGET_DATE.withTimeAtStartOfDay().plusHours(this.HOURS_OFFSET.get(boundary));
            default:
                return TARGET_DATE.withTimeAtStartOfDay().plusHours(this.HOURS_OFFSET.get(boundary));
        }
    }

    public static SleepPeriod create(final DateTime inBedTime){
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

    public static SleepPeriod create(final Period period, final DateTime targetDate){
        if (period == Period.MORNING){
            return SleepPeriod.morning(targetDate);
        }
        if(period == period.AFTERNOON_EVENING){
            return SleepPeriod.afternoonEvening(targetDate);
        }
        return SleepPeriod.night(targetDate);


    }

    public boolean sleepEventInSleepPeriod(final DateTime inBedTime){
        final SleepPeriod eventSleepPeriod = create(inBedTime);
        if (this.PERIOD == eventSleepPeriod.PERIOD){
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

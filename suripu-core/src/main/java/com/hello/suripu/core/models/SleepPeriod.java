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

        public SleepPeriod(final SleepPeriod.Period sleepPeriod) {
            this.PERIOD = sleepPeriod;
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

        public static SleepPeriod night(){
            return new SleepPeriod(Period.NIGHT);
        }

        public static  SleepPeriod afternoonEvening(){
            return new SleepPeriod(Period.AFTERNOON_EVENING);
        }

        public static  SleepPeriod morning(){
            return new SleepPeriod(Period.MORNING);
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

        public DateTime getSleepPeriodTime(final Boundary boundary, final DateTime targetDate){
            switch(PERIOD) {
                case MORNING:
                    return targetDate.withTimeAtStartOfDay().plusHours(this.HOURS_OFFSET.get(boundary));
                case AFTERNOON_EVENING:
                    return targetDate.withTimeAtStartOfDay().plusHours(this.HOURS_OFFSET.get(boundary));
                default:
                    return targetDate.withTimeAtStartOfDay().plusHours(this.HOURS_OFFSET.get(boundary));
            }
        }

        public static SleepPeriod getSleepPeriod(final DateTime inBedTime){
            final Integer inBedHour = inBedTime.getHourOfDay();
            if (inBedHour >= 4 && inBedHour < 12){
                return SleepPeriod.morning();
            } else if (inBedHour >= 12 && inBedHour < 20){
                return SleepPeriod.afternoonEvening();
            } else {
                return SleepPeriod.night();
            }
        }

        public boolean sleepEventInSleepPeriod(final DateTime inBedTime){
            final SleepPeriod eventSleepPeriod = getSleepPeriod(inBedTime);
            if (this.PERIOD == eventSleepPeriod.PERIOD){
                return true;
            }
            return false;
        }
    public static List<SleepPeriod> getAll(){
        final List<SleepPeriod> sleepPeriods = new ArrayList<>();
        sleepPeriods.add(morning());
        sleepPeriods.add(afternoonEvening());
        sleepPeriods.add(night());
        return sleepPeriods;
    }

}

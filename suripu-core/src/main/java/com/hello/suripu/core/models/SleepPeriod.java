package com.hello.suripu.core.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by jarredheinrich on 2/2/17.
 */
public class SleepPeriod {
    private final static int MORNING_START_HOUR = 4;
    private final static int MORNING_IN_BED_END_HOUR = 12;
    private final static int MORNING_DATA_END_HOUR = 20;

    private final static int AFTERNOON_START_HOUR = 12;
    private final static int AFTERNOON_IN_BED_END_HOUR = 20;
    private final static int AFTERNOON_DATA_END_HOUR = 28;

    private final static int NIGHT_START_HOUR = 20;
    private final static int NIGHT_IN_BED_END_HOUR = 28;
    private final static int NIGHT_DATA_END_HOUR = 36;


    public final SleepPeriod.Period period;
    public final ImmutableMap<Boundary, Integer> hoursOffset;
    public final DateTime targetDate;

    public SleepPeriod(final SleepPeriod.Period sleepPeriod, final DateTime targetDate) {
        this.period = sleepPeriod;
        this.targetDate = targetDate.withTimeAtStartOfDay();

        if (sleepPeriod == SleepPeriod.Period.MORNING) {
            this.hoursOffset = ImmutableMap.<Boundary, Integer>builder()
                    .put(Boundary.START,MORNING_START_HOUR)
                    .put(Boundary.END_IN_BED, MORNING_IN_BED_END_HOUR)
                    .put(Boundary.END_DATA, MORNING_DATA_END_HOUR)
                    .build();
        } else if (sleepPeriod == SleepPeriod.Period.AFTERNOON) {
            this.hoursOffset = ImmutableMap.<Boundary, Integer>builder()
                    .put(Boundary.START,AFTERNOON_START_HOUR)
                    .put(Boundary.END_IN_BED, AFTERNOON_IN_BED_END_HOUR)
                    .put(Boundary.END_DATA, AFTERNOON_DATA_END_HOUR)
                    .build();
        } else{
            this.hoursOffset = ImmutableMap.<Boundary, Integer>builder()
                    .put(Boundary.START, NIGHT_START_HOUR)
                    .put(Boundary.END_IN_BED, NIGHT_IN_BED_END_HOUR)
                    .put(Boundary.END_DATA, NIGHT_DATA_END_HOUR)
                    .build();
        }
    }

    public static SleepPeriod createSleepPeriod(final DateTime inBedTime){
        final Integer inBedHour = inBedTime.getHourOfDay();
        if (inBedHour >= MORNING_START_HOUR && inBedHour < MORNING_IN_BED_END_HOUR){
            final DateTime targetDate = inBedTime.withTimeAtStartOfDay();
            return SleepPeriod.morning(targetDate);
        } else if (inBedHour >= AFTERNOON_START_HOUR && inBedHour < AFTERNOON_IN_BED_END_HOUR){
            final DateTime targetDate = inBedTime.withTimeAtStartOfDay();
            return SleepPeriod.afternoonEvening(targetDate);
        } else {
            final DateTime targetDate;
            if (inBedHour < MORNING_START_HOUR){
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
        if(period == period.AFTERNOON){
            return SleepPeriod.afternoonEvening(targetDate);
        }
        return SleepPeriod.night(targetDate);

    }

    public static List<SleepPeriod> createAllSleepPeriods(final DateTime targetDate){
        final ImmutableList<SleepPeriod> sleepPeriods = ImmutableList.<SleepPeriod>builder()
                .add(SleepPeriod.morning(targetDate))
                .add(SleepPeriod.afternoonEvening(targetDate))
                .add(SleepPeriod.night(targetDate))
                .build();
        return sleepPeriods;
    }

    public static SleepPeriod night(final DateTime targetDate){
        return new SleepPeriod(Period.NIGHT, targetDate);
    }

    public static  SleepPeriod afternoonEvening(final DateTime targetDate){
        return new SleepPeriod(Period.AFTERNOON, targetDate);
    }

    public static  SleepPeriod morning(final DateTime targetDate){
        return new SleepPeriod(Period.MORNING, targetDate);
    }

// 2017-02-10|AFTERNOON
// AFTERNOON|2017-02-10

    public enum Period {
        NONE("",-1) {
            public String toString() {
                return "";
            }
        },
        MORNING("morning", 0),
        AFTERNOON("afternoon", 1),
        NIGHT("night", 2);

        private String shortName;
        private int value;

        private Period(String shortName, int value) {
            this.shortName = shortName;
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public String shortName() {
            return this.shortName;
        }


        public static SleepPeriod.Period fromInteger(int value) {
            for (final SleepPeriod.Period period : SleepPeriod.Period.values()) {
                if (period.value == value) {
                    return period;
                }
            }
            return NONE;
        }

        public static Period fromString(String shortName) {
            for(Period period: Period.values()) {
                if(period.shortName.equalsIgnoreCase(shortName)) {
                    return period;
                }
            }

            throw new IllegalArgumentException("Invalid PushNotificationEventType: " + shortName);
        }

        public static List<SleepPeriod.Period> getAllPeriods(){
            final List<SleepPeriod.Period> periods = new ArrayList<>();
            periods.add(Period.MORNING);
            periods.add(Period.AFTERNOON);
            periods.add(Period.NIGHT);
            return periods;
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
            case AFTERNOON:
                return targetDate.withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary));
            default:
                return targetDate.withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary));
        }
    }

    public DateTime getTargetSleepPeriodTime(final Boundary boundary){
        final int  queueProcessorOffset = 2;//hours;
        switch(period) {
            case MORNING:
                return targetDate.withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary));
            case AFTERNOON:
                return targetDate.withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary));
            default:
                return targetDate.withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary));
        }
    }

    public static SleepPeriod create(final DateTime inBedTime){
        final Integer inBedHour = inBedTime.getHourOfDay();
        if (inBedHour >= MORNING_START_HOUR && inBedHour < MORNING_IN_BED_END_HOUR){
            final DateTime targetDate = inBedTime.withTimeAtStartOfDay();
            return SleepPeriod.morning(targetDate);
        } else if (inBedHour >= AFTERNOON_START_HOUR && AFTERNOON_IN_BED_END_HOUR < 20){
            final DateTime targetDate = inBedTime.withTimeAtStartOfDay();
            return SleepPeriod.afternoonEvening(targetDate);
        } else {
            final DateTime targetDate;
            if (MORNING_START_HOUR < 4){
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
        if(period == period.AFTERNOON){
            return SleepPeriod.afternoonEvening(targetDate);
        }
        return SleepPeriod.night(targetDate);

    }

    //get list of last 3 sleep periods
    public static List<SleepPeriod> getTargetPeriods(final DateTime targetDate, final DateTime currentTimeLocal){
        final DateTime currentDate = currentTimeLocal.withTimeAtStartOfDay();
        final SleepPeriod currentSleepPeriod = create(currentTimeLocal);
        int lastSleepPeriodVal = currentSleepPeriod.period.getValue() -1 ;
        final SleepPeriod lastSleepPeriod;
        if (lastSleepPeriodVal < Period.MORNING.getValue()){
            lastSleepPeriodVal = Period.NIGHT.getValue();
            final DateTime lastSleepPeriodDate = currentDate.minusDays(1);
            lastSleepPeriod = create(Period.fromInteger(lastSleepPeriodVal), lastSleepPeriodDate);
        } else {
            lastSleepPeriod = create(Period.fromInteger(lastSleepPeriodVal), currentDate);
        }
        return Collections.EMPTY_LIST;

    }

    public boolean sleepEventInSleepPeriod(final DateTime inBedTime){
        final SleepPeriod eventSleepPeriod = createSleepPeriod(inBedTime);
        if (this.period == eventSleepPeriod.period){
            return true;
        }
        return false;
    }

}

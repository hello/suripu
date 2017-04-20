package com.hello.suripu.core.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
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

    private static int MORNING_ROLLOVER = 7;
    private static int AFTERNOON_ROLLOVER = 19;
    private static int FULLDAY_ROLLOVER = 27;


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
            return SleepPeriod.afternoon(targetDate);
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
            return SleepPeriod.afternoon(targetDate);
        }
        return SleepPeriod.night(targetDate);

    }

    public static List<SleepPeriod> createAllSleepPeriods(final DateTime targetDate){
        final ImmutableList<SleepPeriod> sleepPeriods = ImmutableList.<SleepPeriod>builder()
                .add(SleepPeriod.morning(targetDate))
                .add(SleepPeriod.afternoon(targetDate))
                .add(SleepPeriod.night(targetDate))
                .build();
        return sleepPeriods;
    }

    public SleepPeriod nextSleepPeriod(){
        if (this.period == Period.NIGHT){
           final DateTime nextDay =this.targetDate.plusDays(1);
           return SleepPeriod.morning(nextDay);
        }
        if (this.period == Period.MORNING){
            return SleepPeriod.afternoon(this.targetDate);
        }
        return SleepPeriod.night(this.targetDate);
    }

    public SleepPeriod previousSleepPeriod(){
        if (this.period == Period.MORNING){
            final DateTime previousDay =this.targetDate.minusDays(1);
            return SleepPeriod.night(previousDay);
        }
        if (this.period == Period.AFTERNOON){
            return SleepPeriod.morning(this.targetDate);
        }
        return SleepPeriod.afternoon(this.targetDate);
    }


    public static SleepPeriod night(final DateTime targetDate){
        return new SleepPeriod(Period.NIGHT, targetDate);
    }

    public static  SleepPeriod afternoon(final DateTime targetDate){
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


    public DateTime getSleepPeriodTime(final Boundary boundary, final int offsetMillis){
        switch(period) {
            case MORNING:
                return targetDate.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary)).minusMillis(offsetMillis);
            case AFTERNOON:
                return targetDate.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary)).minusMillis(offsetMillis);
            default:
                return targetDate.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().plusHours(this.hoursOffset.get(boundary)).minusMillis(offsetMillis);
        }
    }

    public Long getSleepPeriodMillis(final Boundary boundary, final int offsetMillis){
        return getSleepPeriodTime(boundary, offsetMillis).getMillis();
    }

    public static List<SleepPeriod> getSleepPeriodQueue(final DateTime targetDate, final DateTime currentTimeLocal){
        final List<SleepPeriod> sleepPeriods = new ArrayList<>();

        //for previous complete day -- roll over 3am
        if (currentTimeLocal.isAfter(targetDate.withTimeAtStartOfDay().plusHours(FULLDAY_ROLLOVER).getMillis())){
            return createAllSleepPeriods(targetDate);
        }
        //for current day upto night period -- roll over 11am - include afternoon after 7pm
        if (currentTimeLocal.isAfter(targetDate.withTimeAtStartOfDay().plusHours(AFTERNOON_ROLLOVER).getMillis())){
            sleepPeriods.add(SleepPeriod.morning(targetDate));
            sleepPeriods.add(SleepPeriod.afternoon(targetDate));

            return sleepPeriods;
        }
        //for current day morning period only roll over 11 am
        if (currentTimeLocal.isAfter(targetDate.withTimeAtStartOfDay().plusHours(MORNING_ROLLOVER).getMillis())){
            sleepPeriods.add(SleepPeriod.morning(targetDate));
            return sleepPeriods;
        }
        //current time before target day. return empty list
        return sleepPeriods;
    }

    public boolean sleepEventInSleepPeriod(final DateTime inBedTime){
        final SleepPeriod eventSleepPeriod = createSleepPeriod(inBedTime);
        if (this.period == eventSleepPeriod.period){
            return true;
        }
        return false;
    }

}

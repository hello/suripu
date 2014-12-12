package com.hello.suripu.app.utils;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 11/19/14.
 */
public class TrackerMotionDataSource implements DataSource<AmplitudeData> {

    private LinkedList<AmplitudeData> dataAfterAutoInsert = new LinkedList<>();
    private int startHourOfDay = 0;
    private int endHourOfDay = 0;
    public static final int DATA_INTERVAL = DateTimeConstants.MILLIS_PER_MINUTE;

    public TrackerMotionDataSource(final List<TrackerMotion> motionsFromDBShortedByTimestamp,
                                   final int startHourOfDay, final int endHourOfDay) {

        final long minAmplitude = getMinAmplitude(motionsFromDBShortedByTimestamp);
        for(final TrackerMotion motion: motionsFromDBShortedByTimestamp) {
            if(motion.value <= 0){
                continue;
            }

            if(this.dataAfterAutoInsert.size() == 0) {
                this.dataAfterAutoInsert.add(trackerMotionToAmplitude(motion));
            }else{
                if(motion.timestamp - this.dataAfterAutoInsert.getLast().timestamp > DATA_INTERVAL) {
                    final List<AmplitudeData> gapData = fillGap(this.dataAfterAutoInsert.getLast().timestamp,
                            motion.timestamp,
                            DATA_INTERVAL,
                            minAmplitude,
                            this.dataAfterAutoInsert.getLast().offsetMillis);
                    this.dataAfterAutoInsert.addAll(gapData);
                }

                this.dataAfterAutoInsert.add(trackerMotionToAmplitude(motion));
            }
        }

        this.startHourOfDay = startHourOfDay;
        this.endHourOfDay = endHourOfDay;
    }

    public static Map.Entry<List<AmplitudeData>, List<AmplitudeData>>
        getPadData(final List<AmplitudeData> data, int padCount, int dataIntervalMS, double defaultValue){

        final LinkedList<AmplitudeData> padFront = new LinkedList<>();
        final LinkedList<AmplitudeData> padRear = new LinkedList<>();

        if(data.size() > 0)
        {
            final AmplitudeData firstData = data.get(0);
            final AmplitudeData lastData = data.get(data.size() - 1);

            for(int i = 0; i < padCount; i++){
                final AmplitudeData frontPad = new AmplitudeData(firstData.timestamp - dataIntervalMS * (i + 1), defaultValue, firstData.offsetMillis);
                final AmplitudeData rearPad = new AmplitudeData(lastData.timestamp + dataIntervalMS * (i + 1), defaultValue, lastData.offsetMillis);
                padFront.add(0, frontPad);
                padRear.add(rearPad);
            }
        }

        return new AbstractMap.SimpleEntry<List<AmplitudeData>, List<AmplitudeData>>(padFront, padRear);

    }

    public static long getMinAmplitude(final List<TrackerMotion> data){
        long minAmplitude = Long.MAX_VALUE;
        for(final TrackerMotion datum:data){
            if(datum.value <= 0){
                continue;
            }

            long amplitude = datum.value;

            if(amplitude < minAmplitude){
                minAmplitude = amplitude;
            }
        }

        if(minAmplitude == Long.MAX_VALUE){
            return 0;
        }

        return minAmplitude;
    }

    /*
    * Insert gap with empty data.
     */
    public static List<AmplitudeData> fillGap(final long gapStartTimestamp, final long gapEndTimestamp,
                                       final int dataIntervalMillis, final double defaultValue,
                                       final int timezoneOffset) {
        final long gapInterval = gapEndTimestamp - gapStartTimestamp;
        int insertCount = (int)(gapInterval / dataIntervalMillis);
        if(gapInterval % dataIntervalMillis == 0){
            insertCount--;
        }

        final ArrayList<AmplitudeData> insertData = new ArrayList<>();
        for(int i = 0; i < insertCount; i++){
            insertData.add(new AmplitudeData(gapStartTimestamp + (i + 1) * dataIntervalMillis, defaultValue, timezoneOffset));
        }

        return insertData;

    }

    /*
    * Convert the TrackerMotion to AmplitudeData which is used by algorithm.
     */
    public static AmplitudeData trackerMotionToAmplitude(final TrackerMotion trackerMotion){
        return new AmplitudeData(trackerMotion.timestamp, Double.valueOf(trackerMotion.value), trackerMotion.offsetMillis);
    }

    /*
    * localUTCDayOfNight is the date of current day's night.
    * For example, the night of 11/1/2014 is some time from the evening of 11/1/2014
    * to some time in 11/2/2014.
     */
    @Override
    public ImmutableList<AmplitudeData> getDataForDate(final DateTime localUTCDayOfNight) {
        if(!localUTCDayOfNight.getZone().equals(DateTimeZone.UTC)) {
            throw new IllegalArgumentException("Local UTC must set to UTC timezone");
        }

        final LinkedList<AmplitudeData> targetList = new LinkedList<>();
        final Map.Entry<DateTime, DateTime> queryBoundary = getStartEndQueryTimeLocalUTC(localUTCDayOfNight, this.startHourOfDay, this.endHourOfDay);

        for(final AmplitudeData amplitudeData:this.dataAfterAutoInsert){
            final DateTime timeFromData = new DateTime(amplitudeData.timestamp, DateTimeZone.forOffsetMillis(amplitudeData.offsetMillis));
            final DateTime localUTCTimeFromData = new DateTime(timeFromData.getYear(), timeFromData.getMonthOfYear(),
                    timeFromData.getDayOfMonth(), timeFromData.getHourOfDay(), timeFromData.getMinuteOfHour(), 0, DateTimeZone.UTC);
            if(localUTCTimeFromData.getMillis() >= queryBoundary.getKey().getMillis() &&
                    localUTCTimeFromData.getMillis() <= queryBoundary.getValue().getMillis()) {
                targetList.add(amplitudeData);
            }
        }


        return ImmutableList.copyOf(targetList);
    }

    /*
    * Get the query boundary in terms of local utc timestamp by the start/end hours of day given.
     */
    public static Map.Entry<DateTime, DateTime> getStartEndQueryTimeLocalUTC(final DateTime localUTCDayOfNight, final int startHourOfDay, final int endHourOfDay) {
        if(!localUTCDayOfNight.getZone().equals(DateTimeZone.UTC)){
            throw new IllegalArgumentException("localUTCDayOfNight must be local time set to UTC");
        }
        DateTime startLocalUTCTime = localUTCDayOfNight.withTimeAtStartOfDay();
        if(startHourOfDay >= 12){
            startLocalUTCTime = startLocalUTCTime.plusHours(startHourOfDay);
        }else{
            startLocalUTCTime = startLocalUTCTime.plusDays(1).plusHours(startHourOfDay);
        }

        DateTime endLocalUTCTime = localUTCDayOfNight.withTimeAtStartOfDay().plusDays(1).plusHours(endHourOfDay);

        if(!endLocalUTCTime.isAfter(startLocalUTCTime)){
            throw new IllegalArgumentException("End time must before start time.");
        }

        return new AbstractMap.SimpleEntry<DateTime, DateTime>(startLocalUTCTime, endLocalUTCTime);
    }
}

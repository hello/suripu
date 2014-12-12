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
    public static final int DATA_INTERVAL = DateTimeConstants.MILLIS_PER_MINUTE;

    public TrackerMotionDataSource(final List<TrackerMotion> motionsFromDBShortedByTimestamp,
                                   final int startHourOfDay, final int endHourOfDay) {

        final int minAmplitude = getMinAmplitude(motionsFromDBShortedByTimestamp);
        for(final TrackerMotion motion: motionsFromDBShortedByTimestamp) {

            if(this.dataAfterAutoInsert.size() == 0) {
                this.dataAfterAutoInsert.add(trackerMotionToAmplitude(motion, minAmplitude));
            }else{
                if(motion.timestamp - this.dataAfterAutoInsert.getLast().timestamp > DATA_INTERVAL) {
                    final List<AmplitudeData> gapData = fillGap(this.dataAfterAutoInsert.getLast().timestamp,
                            motion.timestamp,
                            DATA_INTERVAL,
                            minAmplitude,
                            this.dataAfterAutoInsert.getLast().offsetMillis);
                    this.dataAfterAutoInsert.addAll(gapData);
                }

                this.dataAfterAutoInsert.add(trackerMotionToAmplitude(motion, minAmplitude));
            }
        }

    }


    public static int getMinAmplitude(final List<TrackerMotion> data){
        int minAmplitude = Integer.MAX_VALUE;
        for(final TrackerMotion datum:data){
            if(datum.value <= 0){
                continue;
            }

            int amplitude = datum.value;

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
    public static AmplitudeData trackerMotionToAmplitude(final TrackerMotion trackerMotion, final int defaultValue){
        if(trackerMotion.value < 0){
            return new AmplitudeData(trackerMotion.timestamp, Double.valueOf(defaultValue), trackerMotion.offsetMillis);
        }
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

        for(final AmplitudeData amplitudeData:this.dataAfterAutoInsert){

            targetList.add(amplitudeData);
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

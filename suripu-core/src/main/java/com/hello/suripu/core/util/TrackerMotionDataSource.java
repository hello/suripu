package com.hello.suripu.core.util;

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

    public TrackerMotionDataSource(final List<TrackerMotion> motionsFromDBShortedByTimestamp) {

        //final List<TrackerMotion> positiveData = retainPositiveAmplitudes(motionsFromDBShortedByTimestamp);
        final List<AmplitudeData> raw = new ArrayList<>();
        for(final TrackerMotion motion: motionsFromDBShortedByTimestamp) {
            raw.add(new AmplitudeData(motion.timestamp, motion.value, motion.offsetMillis));
        }
        this.dataAfterAutoInsert.addAll(com.hello.suripu.algorithm.utils.DataUtils.fillMissingValuesAndMakePositive(raw, DateTimeConstants.MILLIS_PER_MINUTE));

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

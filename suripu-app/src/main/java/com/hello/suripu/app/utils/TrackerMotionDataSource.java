package com.hello.suripu.app.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedInteger;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
        for(final TrackerMotion motion: motionsFromDBShortedByTimestamp) {
            if(this.dataAfterAutoInsert.size() == 0) {
                this.dataAfterAutoInsert.add(trackerMotionToAmplitude(motion));
            }else{
                if(motion.timestamp - this.dataAfterAutoInsert.getLast().timestamp >= 2 * DATA_INTERVAL) {
                    final List<AmplitudeData> gapData = fillGap(this.dataAfterAutoInsert.getLast().timestamp,
                            motion.timestamp,
                            DATA_INTERVAL,
                            0,
                            this.dataAfterAutoInsert.getLast().offsetMillis);
                    this.dataAfterAutoInsert.addAll(gapData);
                }

                this.dataAfterAutoInsert.add(trackerMotionToAmplitude(motion));
            }
        }

        this.startHourOfDay = startHourOfDay;
        this.endHourOfDay = endHourOfDay;
    }

    /*
    * Insert gap with empty data.
     */
    public List<AmplitudeData> fillGap(final long gapStartTimestamp, final long gapEndTimestamp,
                                       final int dataIntervalMillis, final double defaultValue,
                                       final int timezoneOffset) {
        final long gapInterval = gapEndTimestamp - gapStartTimestamp;
        final int insertCount = (int)(gapInterval / dataIntervalMillis);

        final ArrayList<AmplitudeData> insertData = new ArrayList<>();
        for(int i = 0; i < insertCount; i++){
            insertData.add(new AmplitudeData(gapStartTimestamp + (i + 1) * dataIntervalMillis, defaultValue, timezoneOffset));
        }

        return insertData;

    }

    /*
    * Convert the TrackerMotion to AmplitudeData which is used by algorithm.
     */
    public AmplitudeData trackerMotionToAmplitude(final TrackerMotion trackerMotion){
        double amplitude = 0;
        if(trackerMotion.value < 0){
            long unsignedLongAmplitude = UnsignedInteger.fromIntBits(trackerMotion.value).longValue();
            amplitude = Math.sqrt(unsignedLongAmplitude);
        }else{
            amplitude = Math.sqrt(trackerMotion.value);
        }
        return new AmplitudeData(trackerMotion.timestamp, amplitude, trackerMotion.offsetMillis);
    }

    @Override
    public ImmutableList<AmplitudeData> getDataForDate(final DateTime day) {
        return ImmutableList.copyOf(this.dataAfterAutoInsert);
    }
}

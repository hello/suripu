package com.hello.suripu.algorithm.utils;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 6/11/14.
 */
public class DataCutter implements AmplitudeDataPreprocessor {
    private final DateTime startTimeLocalUTC;
    private final DateTime endTimeLocalUTC;

    public DataCutter(final DateTime startTimeLocalUTC, final DateTime endTimeLocalUTC){
        this.startTimeLocalUTC = startTimeLocalUTC;
        this.endTimeLocalUTC = endTimeLocalUTC;

    }

    @Override
    public ImmutableList<AmplitudeData> process(final List<AmplitudeData> rawData) {
        final LinkedList<AmplitudeData> processedData = new LinkedList<AmplitudeData>();
        final long startTimestampUTC = this.startTimeLocalUTC.getMillis();
        final long endTimestampUTC = this.endTimeLocalUTC.getMillis();

        for(final AmplitudeData datum:rawData){
            final DateTime timeFromData = new DateTime(datum.timestamp, DateTimeZone.forOffsetMillis(datum.offsetMillis));
            final DateTime localUTCTimeFromData = new DateTime(timeFromData.getYear(),
                    timeFromData.getMonthOfYear(),
                    timeFromData.getDayOfMonth(),
                    timeFromData.getHourOfDay(),
                    timeFromData.getMinuteOfHour(),
                    0,
                    DateTimeZone.UTC);

            if(localUTCTimeFromData.getMillis() >= startTimestampUTC && localUTCTimeFromData.getMillis() <= endTimestampUTC){
                processedData.add(datum);
            }
        }

        return ImmutableList.copyOf(processedData);
    }
}

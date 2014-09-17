package com.hello.suripu.algorithm.event;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by pangwu on 9/5/14.
 */
public class SleepCycleAlgorithm {
    private DataSource<AmplitudeData> dataSource;
    private int slidingWindowSizeInMinutes = 15;


    public SleepCycleAlgorithm(final DataSource<AmplitudeData> dataSource, final int slidingWindowSizeInMinutes){
        this.dataSource = dataSource;
        this.slidingWindowSizeInMinutes = slidingWindowSizeInMinutes;
    }


    protected float getDensity(final List<AmplitudeData> buffer){

        if(buffer.size() == 0){
            return 0f;
        }

        int count = 0;
        for(final AmplitudeData datum:buffer){
            if(datum.amplitude != 0){
                count++;
            }
        }

        return (float)count / ((buffer.get(buffer.size() - 1).timestamp - buffer.get(0).timestamp) / (60 * 1000));
    }



    public ImmutableList<Segment> getCycles(final DateTime dateTime){

        float minDensity = 1f / 5f;
        final ArrayList<Float> densities = new ArrayList<Float>();
        final LinkedList<AmplitudeData> eventBuffer = new LinkedList<AmplitudeData>();

        final ImmutableList<AmplitudeData> data = this.dataSource.getDataForDate(dateTime);


        for(final AmplitudeData datum:data){
            if(eventBuffer.size() == slidingWindowSizeInMinutes){

                densities.add(getDensity(eventBuffer));
                eventBuffer.removeFirst();

            }

            eventBuffer.add(datum);
        }

        densities.add(getDensity(eventBuffer));


        final Float[] densityArray =  densities.toArray(new Float[0]);
        Arrays.sort(densityArray, new Comparator<Float>() {
            @Override
            public int compare(final Float aFloat, final Float aFloat2) {
                return aFloat.compareTo(aFloat2);
            }
        });

        float actualMaxDensity = densityArray[densityArray.length - 1];
        if(actualMaxDensity < minDensity && densityArray.length > 2){
            minDensity = (densityArray[densityArray.length - 1] - densityArray[0]) / 2f;
        }

        long segmentStart = -1;
        long segmentEnd = segmentStart;
        eventBuffer.clear();

        ArrayList<Segment> segments = new ArrayList<Segment>();

        for(final AmplitudeData datum:data){
            if(eventBuffer.size() == slidingWindowSizeInMinutes){

                float density = getDensity(eventBuffer);
                if(density >= minDensity) {
                    if (segmentStart == -1) {
                        segmentStart = eventBuffer.getFirst().timestamp;
                    }

                    segmentEnd = eventBuffer.getLast().timestamp;


                }else{
                    if(segmentStart > 0 && segmentEnd > segmentStart) {
                        final Segment segment = new Segment();
                        segment.setStartTimestamp(segmentStart);
                        segment.setEndTimestamp(segmentEnd);

                        segments.add(segment);
                    }

                    segmentStart = -1;
                    segmentEnd = segmentStart;
                }

                eventBuffer.removeFirst();

            }

            eventBuffer.add(datum);
        }


        if(segmentStart > 0 && segmentEnd > segmentStart){
            final Segment segment = new Segment();
            segment.setStartTimestamp(segmentStart);
            segment.setEndTimestamp(segmentEnd);

            segments.add(segment);
        }


        return ImmutableList.copyOf(segments);
    }


    public DateTime getSmartAlarmTimeUTC(final List<Segment> sleepCycles, final DateTime alarmTime, int advanceMinutes){

        final long alarmDeadline = alarmTime.getMillis();
        final Segment lastCycle = sleepCycles.get(sleepCycles.size() - 1);
        long deepSleepMoment = lastCycle.getEndTimestamp() + advanceMinutes * DateTimeConstants.MILLIS_PER_MINUTE;
        long dataCollectionMoment = alarmDeadline - advanceMinutes * DateTimeConstants.MILLIS_PER_MINUTE;
        DateTime smartAlarmTime = new DateTime(alarmDeadline, DateTimeZone.UTC);

        int possibleSpanInMinutes = (int)(deepSleepMoment - dataCollectionMoment) / DateTimeConstants.MILLIS_PER_MINUTE;
        final Random random = new Random();

        if(possibleSpanInMinutes > 0) {

            smartAlarmTime = smartAlarmTime.minusMinutes(advanceMinutes).plusMinutes(random.nextInt(possibleSpanInMinutes) + 1);
        }else{
            // User already in deep sleep.
            long sleepCycleLength = (long)(1.5 * DateTimeConstants.MILLIS_PER_HOUR);
            long cycleNumberInTheMiddle = (dataCollectionMoment- lastCycle.getEndTimestamp()) / sleepCycleLength;

            // It is possible that cycleNumberInTheMiddle > 0. In that case we need to guess the cycle.
            long nextLightSleepMoment = lastCycle.getEndTimestamp() + cycleNumberInTheMiddle * sleepCycleLength;

            if(nextLightSleepMoment > dataCollectionMoment && nextLightSleepMoment < alarmDeadline){
                smartAlarmTime = new DateTime(nextLightSleepMoment, DateTimeZone.UTC);
            }else {
                smartAlarmTime = smartAlarmTime.minusMinutes(5).plusMinutes(random.nextInt(5) + 1);
            }
        }

        return smartAlarmTime;
    }

}

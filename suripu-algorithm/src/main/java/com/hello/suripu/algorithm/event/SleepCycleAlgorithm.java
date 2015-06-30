package com.hello.suripu.algorithm.event;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by pangwu on 9/5/14.
 *
 * Given any data source (CSV, Postgres, etc.) it will output the sleep cycles as a list of segments.
 *
 */
public class SleepCycleAlgorithm {
    private final static Logger LOGGER = LoggerFactory.getLogger(SleepCycleAlgorithm.class);

    private DataSource<AmplitudeData> dataSource;
    private int slidingWindowSizeInMinutes = 15;


    public SleepCycleAlgorithm(final DataSource<AmplitudeData> dataSource, final int slidingWindowSizeInMinutes){
        this.dataSource = dataSource;
        if(slidingWindowSizeInMinutes <= 0){
            throw new IllegalArgumentException("slidingWindowSizeInMinutes should be greater than 0");
        }
        this.slidingWindowSizeInMinutes = slidingWindowSizeInMinutes; // TODO: should not allow 0;
    }


    /**
     * Count the % of events in the window defined by the buffer
     * @param buffer
     * @return
     */
    protected static float getDensity(final List<AmplitudeData> buffer){

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


    /**
     * This is the main function.
     * Generates list of Segments based on the data from the data source
     * @param dateTime date of sleeping night
     * @param minDensity initial threshold for light sleep detection
     * @return
     */
    public ImmutableList<Segment> getCycles(final DateTime dateTime, final float minDensity){

        final ArrayList<Float> densities = new ArrayList<Float>();
        final LinkedList<AmplitudeData> eventBuffer = new LinkedList<AmplitudeData>();  // sliding window

        final ImmutableList<AmplitudeData> data = this.dataSource.getDataForDate(dateTime);


        for(final AmplitudeData datum: data){
            if(eventBuffer.size() == slidingWindowSizeInMinutes){

                densities.add(getDensity(eventBuffer));
                eventBuffer.removeFirst();

            }

            eventBuffer.add(datum);
        }

        // Add for the remaining state
        densities.add(getDensity(eventBuffer));

        float actualDensityThreshold = computeMinDensity(densities, minDensity);

        final List<Segment> segments = generateSegmentsFromAmplitudeData(data, actualDensityThreshold, slidingWindowSizeInMinutes);
        return ImmutableList.copyOf(segments);
    }


    /**
     * Finds the best time to wake up the user based on their sleep cycles and alarm settings
     * @param sleepCycles
     * @param dataCollectionMoment
     * @param alarmDeadlineUTC
     * @return
     */
    public static DateTime getSmartAlarmTimeUTC(final List<Segment> sleepCycles,
                                         long dataCollectionMoment, long minAlarmTimeUTC, long alarmDeadlineUTC){

        if(minAlarmTimeUTC >= alarmDeadlineUTC){
            return new DateTime(alarmDeadlineUTC);
        }
        
        final Segment lastCycle = sleepCycles.get(sleepCycles.size() - 1);
        long deepSleepMoment = lastCycle.getEndTimestamp() + 20 * DateTimeConstants.MILLIS_PER_MINUTE;

        DateTime smartAlarmTime = new DateTime(alarmDeadlineUTC, DateTimeZone.UTC);

        final int possibleSpanInMinutes = (int)(deepSleepMoment - dataCollectionMoment) / DateTimeConstants.MILLIS_PER_MINUTE;
        final Random random = new Random();

        if(possibleSpanInMinutes > 0) {
            LOGGER.info("User still in light sleep. Next deep sleep moment: " + new DateTime(deepSleepMoment));
            smartAlarmTime = new DateTime(dataCollectionMoment, DateTimeZone.UTC).plusMinutes(random.nextInt(possibleSpanInMinutes) + 1);
        }else{
            // User already in deep sleep.
            long sleepCycleLength = (long)(1.5 * DateTimeConstants.MILLIS_PER_HOUR);
            long cycleNumberInTheMiddle = (alarmDeadlineUTC - lastCycle.getEndTimestamp()) / sleepCycleLength;

            // It is possible that cycleNumberInTheMiddle > 0. In that case we need to guess the cycle.
            long nextLightSleepMoment = lastCycle.getEndTimestamp() + cycleNumberInTheMiddle * sleepCycleLength;
            LOGGER.info("User already in deep sleep. Next light sleep moment: " + new DateTime(nextLightSleepMoment));

            // the smart alarm should happens at least 10 minutes later, so sense will have enough time to retrieve it
            if(nextLightSleepMoment >= minAlarmTimeUTC &&
                    nextLightSleepMoment < alarmDeadlineUTC){
                smartAlarmTime = new DateTime(nextLightSleepMoment, DateTimeZone.UTC);
            }else {
                // Give fallback random more space, so it doesn't always ring near the end
                final int fakeSmartSpanMin = (int)(alarmDeadlineUTC - minAlarmTimeUTC) / 2 / DateTimeConstants.MILLIS_PER_MINUTE;
                smartAlarmTime = smartAlarmTime.minusMinutes(fakeSmartSpanMin).plusMinutes(random.nextInt(fakeSmartSpanMin));
            }
        }

        LOGGER.debug("Smart alarm time: " + smartAlarmTime);

        return smartAlarmTime;
    }


    /**
     * Find the best density to generate sleep cycles from the actual data
     * In this case, we take the average value
     * @param densities
     * @param defaultMinDensity
     * @return
     */
    public static float computeMinDensity(final ArrayList<Float> densities, float defaultMinDensity) {
        final Float[] densityArray =  densities.toArray(new Float[0]);
        Arrays.sort(densityArray, new Comparator<Float>() {
            @Override
            public int compare(final Float aFloat, final Float aFloat2) {
                return aFloat.compareTo(aFloat2);
            }
        });

        float actualMaxDensity = densityArray[densityArray.length - 1];
        if(actualMaxDensity < defaultMinDensity && densityArray.length > 2){
            return (densityArray[densityArray.length - 1] - densityArray[0]) / 2f;
        }

        return defaultMinDensity;
    }


    /**
     * Generates Segment of sleep cycles based on amplitude data and given density
     * @param data
     * @param minDensity
     * @return
     */
    public static List<Segment> generateSegmentsFromAmplitudeData(final List<AmplitudeData> data, final float minDensity, int slidingWindowSizeInMinutes) {

        long segmentStart = -1;
        long segmentEnd = segmentStart;

        final List<Segment> segments = new ArrayList<>();
        final LinkedList<AmplitudeData> eventBuffer = new LinkedList<AmplitudeData>();  // sliding window

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

        return segments;
    }
}

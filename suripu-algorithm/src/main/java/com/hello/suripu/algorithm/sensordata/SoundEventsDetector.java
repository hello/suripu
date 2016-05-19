package com.hello.suripu.algorithm.sensordata;

import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 12/10/14.
 */
public class SoundEventsDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundEventsDetector.class);

    private static final long MINUTE_IN_MILLIS = 60000L;
    public static final float PEAK_DISTURBANCE_THRESHOLD = 50.0f;
    public static final float PEAK_ENERGY_THRESHOLD = 60.0f;

    private final int approxQuietTimeStart;
    private final int approxQuietTimeEnd;
    private final int smoothingDegree;

    public SoundEventsDetector(final int approxQuietTimeStart, final int approxQuietTimeEnd, final int smoothingDegree) {
        this.approxQuietTimeStart = approxQuietTimeStart;
        this.approxQuietTimeEnd = approxQuietTimeEnd;
        this.smoothingDegree = smoothingDegree;
    }

    /**
     *  This method detects sound spikes during sleep
     *
     * @param rawDataMinutes raw sound data, one value per minute
     * @return list of sound segments
     */
    public LinkedList<Segment> process(final LinkedList<AmplitudeData> rawDataMinutes, final float audioThreshold) {

        LOGGER.debug("Data Start time: {}", rawDataMinutes.get(0).timestamp);
        LOGGER.debug("Data End time: {}", rawDataMinutes.getLast().timestamp);

        // compute average of raw sound data
        double totalValue = 0.0;
        for (final AmplitudeData value : rawDataMinutes) {
            totalValue += value.amplitude;
        }
        final double avgPeakDisturbance = totalValue / (double) rawDataMinutes.size() + 1.0;


        // keeps track loud sounds (> average sound energy) in time windows determined by smoothing degree
        final Map<DateTime, AmplitudeData> peakSoundWindows = new HashMap<>();

        // tracks the value and timestamp of the last added peak
        long lastAddedTimestamp = 0;
        double lastAddedValue = 0.0;
        final List<DateTime> addedTimeBuckets = new ArrayList<>();
        final long minInterval = this.smoothingDegree * MINUTE_IN_MILLIS;

        for (final AmplitudeData datum : rawDataMinutes) {

            if (datum.amplitude < 60.0f) {
                continue;
            }

            final DateTime dataTime = new DateTime(datum.timestamp, DateTimeZone.UTC).plusMillis(datum.offsetMillis);

            // ignore data outside of (11pm to 7am, changeable) for sound disturbance
            final int dataHour = dataTime.getHourOfDay();
            if ((dataHour > this.approxQuietTimeEnd && dataHour <= this.approxQuietTimeStart) || (dataHour > this.approxQuietTimeEnd )) {
                continue;
            }

            final int minuteBucket = (dataTime.getMinuteOfHour() / this.smoothingDegree) * this.smoothingDegree;
            final DateTime timeBucket = dataTime.withMinuteOfHour(minuteBucket).withSecondOfMinute(0).withMillisOfSecond(0);

            // add new sound disturbance
            if (!peakSoundWindows.containsKey(timeBucket)) {

                // check if the last added peak is too recent
                if (!addedTimeBuckets.isEmpty() && (datum.timestamp - lastAddedTimestamp) < minInterval) {
                    if (datum.amplitude <= lastAddedValue) {
                        continue;
                    } else {
                        // this new peak is louder than the last bucket's peak, delete the previous peak
                        final int lastIndex = addedTimeBuckets.size() - 1;
                        peakSoundWindows.remove(addedTimeBuckets.get(lastIndex));
                        addedTimeBuckets.remove(lastIndex);
                    }
                }

                peakSoundWindows.put(timeBucket, datum);

                lastAddedTimestamp = datum.timestamp;
                lastAddedValue = datum.amplitude;
                addedTimeBuckets.add(timeBucket);
            }

            // there's a louder noise in this time window
            if (datum.amplitude > peakSoundWindows.get(timeBucket).amplitude) {
                peakSoundWindows.put(timeBucket, datum);

                lastAddedTimestamp = datum.timestamp;
                lastAddedValue = datum.amplitude;
            }
        }

        final List<AmplitudeData> sortedAmplitudes = Ordering.natural().reverse().sortedCopy(peakSoundWindows.values());

        final LinkedList<Segment> soundSegments = new LinkedList<>();
        for (final AmplitudeData datum : sortedAmplitudes) {
            if (datum.amplitude <= audioThreshold) {
                break;
            }
            final DateTime dateTime = new DateTime(datum.timestamp, DateTimeZone.UTC).plusMillis(datum.offsetMillis);
            LOGGER.debug("debug=sound-event-value value={} time={}", datum.amplitude, dateTime);
            soundSegments.add(new Segment(datum.timestamp, datum.timestamp + MINUTE_IN_MILLIS, datum.offsetMillis));
        }

        return soundSegments;
    }


}
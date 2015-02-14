package com.hello.suripu.algorithm.sensordata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.GaussianSmoother;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 12/10/14.
 */
public class SoundEventsDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundEventsDetector.class);

    private final int approxQuietTimeStart;
    private final int approxQuietTimeEnd;
    private final int smoothingDegree;

    public SoundEventsDetector(final int approxQuietTimeStart, final int approxQuietTimeEnd, final int smoothingDegree) {
        this.approxQuietTimeStart = approxQuietTimeStart;
        this.approxQuietTimeEnd = approxQuietTimeEnd;
        this.smoothingDegree = smoothingDegree;
    }

    /**
     *  This method detects sound spikes duringh sleep
     *
     *  Step 1: data is gaussian-smoothed with a window of 20 minutes, to reduce effects of light fluctuations
     *  Step 2: scan data in chronological order, and detect periods of light
     *  Step 3: annotate each period:
     *    LIGHT_SPIKE if light is on for a short duration during the night,
     *    LIGHTS_OUT if light is on for a long time and it's the last period betw sunset and sunrise,
     *    DAYLIGHT if light is detected and hour of day is > sunrise hour
     *    LOW_DAYLIGHT if daylight detected, but light level is low, probably a darker room
     *    SUNLIGHT_SPIKE (not implemented yet)
     *
     * @param rawDataMinutes raw light data, one value per minute
     * @return list of light segments
     */
    public LinkedList<Segment> process(final LinkedList<AmplitudeData> rawDataMinutes) {

        final GaussianSmoother smoother = new GaussianSmoother(smoothingDegree);
        final ImmutableList<AmplitudeData> smoothedData = smoother.process(rawDataMinutes);

        LOGGER.debug("Data Start time: {}", rawDataMinutes.get(0).timestamp);
        LOGGER.debug("Data End time: {}", rawDataMinutes.getLast().timestamp);

        // compute average of smoothed sound data
        double totalValue = 0.0;
        for (AmplitudeData value : smoothedData) {
            totalValue += value.amplitude;
        }
        final double smoothedAverage = totalValue / (double) smoothedData.size();

        final LinkedList<Segment> soundSegments = new LinkedList<>();

        long startTimestamp = 0;
        long endTimestamp = 0;
        int offsetMillis = 0;

        // keeps track loud sounds (> average sound energy) in 20 minutes window.
        final Map<DateTime, AmplitudeData> peakSoundWindows = new HashMap<>();
        for (final AmplitudeData datum : smoothedData) {

            if (datum.amplitude < smoothedAverage) {
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
            if (!peakSoundWindows.containsKey(timeBucket)) {
                peakSoundWindows.put(timeBucket, datum);
            }

            if (peakSoundWindows.get(timeBucket).amplitude < datum.amplitude) {
                peakSoundWindows.put(timeBucket, datum);
            }
        }

        final List<AmplitudeData> sortedAmplitudes = Ordering.natural().reverse().sortedCopy(peakSoundWindows.values());
        for (final AmplitudeData datum : sortedAmplitudes) {
            soundSegments.add(new Segment(datum.timestamp, datum.timestamp + 60000L, datum.offsetMillis));
            if (soundSegments.size() >= 5) {
                break;
            }
        }

        return soundSegments;
    }


}
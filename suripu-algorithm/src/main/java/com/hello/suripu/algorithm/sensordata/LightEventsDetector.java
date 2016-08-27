package com.hello.suripu.algorithm.sensordata;

import com.google.common.collect.ImmutableList;
import com.google.common.base.Optional;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.LightSegment;
import com.hello.suripu.algorithm.utils.GaussianSmoother;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by kingshy on 12/10/14.
 */
public class LightEventsDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LightEventsDetector.class);

    private static final long LIGHT_SPIKE_DURATION_THRESHOLD = 3 * 60000; // 3 minutes

    private final int approxSunsetHour;
    private final int approxSunriseHour;
    private final double noLightThreshold;
    private final int smoothingDegree;

    public LightEventsDetector(final int approxSunriseHour, final int approxSunsetHour, final double noLightThreshold, final int smoothingDegree) {
        this.approxSunriseHour = approxSunriseHour;
        this.approxSunsetHour = approxSunsetHour;
        this.noLightThreshold = noLightThreshold;
        this.smoothingDegree = smoothingDegree;
    }

    /**
     *  This method detects periods of time when light value > some threshold.
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
    public LinkedList<LightSegment> process(final LinkedList<AmplitudeData> rawDataMinutes, final Optional<Long> sleepTime) {

        final GaussianSmoother smoother = new GaussianSmoother(smoothingDegree);
        final ImmutableList<AmplitudeData> smoothedData = smoother.process(rawDataMinutes);

        LOGGER.debug("Data Start time: {}", rawDataMinutes.get(0).timestamp);
        LOGGER.debug("Data End time: {}", rawDataMinutes.getLast().timestamp);

        // get windows of light, and assign a label
        final LinkedList<LightSegment> lightSegments = new LinkedList<>();

        long startTimestamp = 0;
        long endTimestamp = 0;
        int offsetMillis = 0;
        List<Double> buffer = new ArrayList<>();

        for (final AmplitudeData datum : smoothedData) {
            if (datum.amplitude < noLightThreshold) {
                if (startTimestamp > 0) {
                    // Lights off
                    final LightSegment.Type segmentType = getLightSegmentType(startTimestamp, endTimestamp, offsetMillis, buffer, sleepTime);
                    final LightSegment segment = new LightSegment(startTimestamp, endTimestamp, offsetMillis, segmentType);
                    LOGGER.info("start {}, end {}", startTimestamp, endTimestamp);
                    
                    if (segmentType == LightSegment.Type.LIGHTS_OUT && lightSegments.size() > 0) {
                        // if previous label is LIGHTS_OUT, unset it
                        final LightSegment lastSegment = lightSegments.removeLast();
                        final LightSegment updatedLastSegment = LightSegment.updateWithSegmentType(lastSegment, LightSegment.Type.NONE);
                        lightSegments.add(updatedLastSegment);
                    }

                    lightSegments.add(segment);

                    startTimestamp = 0;
                    endTimestamp = 0;
                    buffer = new ArrayList<>();
                }
                continue;
            }

            if (startTimestamp == 0) {
                // Light value > 0 at this minute
                startTimestamp = datum.timestamp;
            }

            endTimestamp = datum.timestamp;
            offsetMillis = datum.offsetMillis;
            buffer.add(datum.amplitude);

        }
        return lightSegments;
    }

    private LightSegment.Type getLightSegmentType(final long startTimestamp, final long endTimestamp, final int offsetMillis, final List<Double> segmentValues, final Optional<Long> sleepTime) {

        LightSegment.Type segmentType = LightSegment.Type.NONE;
        boolean qualifiedLightsOutTime = false;
        boolean useSleepTime = false;

        if (sleepTime.isPresent()){
            useSleepTime = true;
            if (sleepTime.get() > endTimestamp + DateTimeConstants.MILLIS_PER_MINUTE * 10) {
                qualifiedLightsOutTime = true;
            }
        }

        final int startHour = getTimestampLocalHour(startTimestamp, offsetMillis);
        final int endHour = getTimestampLocalHour(endTimestamp, offsetMillis);

        if ((startHour < approxSunriseHour || startHour >= approxSunsetHour) && (endHour > approxSunsetHour || endHour < approxSunriseHour)) {
            // night-time
            if ((endTimestamp - startTimestamp) < LIGHT_SPIKE_DURATION_THRESHOLD) {
                // short light duration, consider it as an anomaly
                segmentType = LightSegment.Type.LIGHT_SPIKE;
            } else if (qualifiedLightsOutTime || !useSleepTime ) {
                //if no user sleeptime available - defaults to previous behavior (for voting algorithm), else lights out event must be within 10 mins of sleep
                segmentType = LightSegment.Type.LIGHTS_OUT;
            } else{
                LOGGER.debug("event=lights-out-event-too-late event_end_time={}", endTimestamp);
            }
        } else if (startHour >= approxSunriseHour && endHour > approxSunriseHour) {
            // daytime
            segmentType = LightSegment.Type.DAYLIGHT;

            final DescriptiveStatistics stats = this.getStats(segmentValues);
            final double meanMedianDiff = Math.abs(stats.getMean() - stats.getPercentile(50.0)); // avg - median

            if (stats.getStandardDeviation() < meanMedianDiff && meanMedianDiff < stats.getStandardDeviation()) {
                // not getting that huge n-shape for regular daylight, consider this a "dark" room
                segmentType = LightSegment.Type.LOW_DAYLIGHT;
            }

            // TODO: get sudden daylight spike when blinds/windows/doors are opened
        }
        return segmentType;
    }

    private DescriptiveStatistics getStats(List<Double> data) {
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final Double value : data) {
            stats.addValue(value);
        }
        return stats;
    }

    private int getTimestampLocalHour(final long timestamp, final int offsetMillis) {
        return (new DateTime(timestamp, DateTimeZone.UTC).plusMillis(offsetMillis)).getHourOfDay();
    }

}
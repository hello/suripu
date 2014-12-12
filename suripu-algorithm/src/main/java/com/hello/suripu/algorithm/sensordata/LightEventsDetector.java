package com.hello.suripu.algorithm.sensordata;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.LightLabel;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.GaussianSmoother;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
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

    private static long LIGHT_SPIKE_DURATION_THRESHOLD = 3 * 60000; // 3 minutes

    private int approxSunsetHour;
    private int approxSunriseHour;
    private double noLightThreshold;
    private int smoothingDegree;

    public LightEventsDetector(final int approxSunriseHour, final int approxSunsetHour, final double noLightThreshold, final int smoothingDegree) {
        this.approxSunriseHour = approxSunriseHour;
        this.approxSunsetHour = approxSunsetHour;
        this.noLightThreshold = noLightThreshold;
        this.smoothingDegree = smoothingDegree;
    }

    public LinkedList<Segment> process(final LinkedList<AmplitudeData> rawDataMinutes) {

        final GaussianSmoother smoother = new GaussianSmoother(smoothingDegree);
        final ImmutableList<AmplitudeData> smoothedData = smoother.process(rawDataMinutes);

        LOGGER.debug("Data Start time: {}", rawDataMinutes.get(0).timestamp);
        LOGGER.debug("Data End time: {}", rawDataMinutes.getLast().timestamp);
        // get windows of light, and assign a label
        final LinkedList<Segment> lightSegments = new LinkedList<>();

        long startTimestamp = 0;
        long endTimestamp = 0;
        int offsetMillis = 0;
        List<Double> buffer = new ArrayList<>();


        for (final AmplitudeData datum : smoothedData) {
            if (datum.amplitude < noLightThreshold) {
                if (startTimestamp > 0) {
                    final Segment segment = new Segment(startTimestamp, endTimestamp, offsetMillis);
                    final LightLabel.Type segmentLabel = getLabel(segment, buffer);
                    segment.setLabel(segmentLabel.toString());

                    if (segmentLabel == LightLabel.Type.LIGHTS_OUT && lightSegments.size() > 0) {
                        lightSegments.getLast().setLabel(LightLabel.Type.NONE.toString());
                    }

                    lightSegments.add(segment);

                    startTimestamp = 0;
                    endTimestamp = 0;
                    buffer = new ArrayList<>();
                }
                continue;
            }

            if (startTimestamp == 0) {
                startTimestamp = datum.timestamp;
            }

            endTimestamp = datum.timestamp;
            offsetMillis = datum.offsetMillis;
            buffer.add(datum.amplitude);

        }
        return lightSegments;
    }

    private LightLabel.Type getLabel(final Segment segment, final List<Double> segmentValues) {
        LightLabel.Type label = LightLabel.Type.NONE;
        final int offsetMillis = segment.getOffsetMillis();
        final int startHour = getTimestampLocalHour(segment.getStartTimestamp(), offsetMillis);
        final int endHour = getTimestampLocalHour(segment.getEndTimestamp(), offsetMillis);

        if ((startHour < approxSunriseHour || startHour >= approxSunsetHour) && (endHour > approxSunsetHour || endHour < approxSunriseHour)) {
            // night-time
            if (segment.getDuration() < LIGHT_SPIKE_DURATION_THRESHOLD) {
                label = LightLabel.Type.LIGHT_SPIKE;
            } else {
                label = LightLabel.Type.LIGHTS_OUT;
            }
        } else if (startHour >= approxSunriseHour && endHour > approxSunriseHour) {
            // daytime
            label = LightLabel.Type.DAYLIGHT;

            final DescriptiveStatistics stats = this.getStats(segmentValues);
            final double meanMedianDiff = Math.abs(stats.getMean() - stats.getPercentile(50.0));

            if (stats.getStandardDeviation() < meanMedianDiff && meanMedianDiff < stats.getStandardDeviation()) {
                label = LightLabel.Type.LOW_DAYLIGHT;
            }

            // TODO: get sudden daylight spike
        }
        return label;
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

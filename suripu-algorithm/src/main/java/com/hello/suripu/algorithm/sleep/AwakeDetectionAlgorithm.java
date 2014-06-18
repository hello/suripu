package com.hello.suripu.algorithm.sleep;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.DataCutter;
import com.hello.suripu.algorithm.utils.MaxAmplitudeAggregator;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 6/11/14.
 *
 * The Awake Period detection algorithm works based on the idea below:
 *
 * The algorithm tries to found two "n Shapes", which are awake periods before and after sleep
 * based on two optimization criteria:
 *
 * 1) Minimizes the disruption error in both awake and quiet period.
 * 2) Maximizes the length of awake period.
 *
 */
public class AwakeDetectionAlgorithm extends SleepDetectionAlgorithm {

    private enum PaddingMode { PAD_EARLY, PAD_LATE }

    public AwakeDetectionAlgorithm(final DataSource<AmplitudeData> dataSource, final int smootheWindowMillis){
        super(dataSource, smootheWindowMillis);
    }

    @Override
    public Segment getSleepPeriod(final DateTime dateOfTheNight) throws AlgorithmException {
        final ImmutableList<AmplitudeData> rawData = getDataSource().getDataForDate(dateOfTheNight);
        if(rawData.size() == 0){
            throw new AlgorithmException("No data available for date: " + dateOfTheNight);
        }

        // Step 1: Aggregate the data based on a 10 minute interval.
        final AmplitudeDataPreprocessor smoother = new MaxAmplitudeAggregator(getSmoothWindow());
        final ImmutableList<AmplitudeData> smoothedData = smoother.process(rawData);

        // Step 2: Generate two folds of data, first fold for fall asleep detection
        // The second fold for wake up detection.
        final AmplitudeDataPreprocessor cutBefore4am = new DataCutter(dateOfTheNight.withTimeAtStartOfDay().plusHours(12),
                dateOfTheNight.withTimeAtStartOfDay().plusDays(1).plusHours(4));

        final AmplitudeDataPreprocessor cutAfter4am = new DataCutter(dateOfTheNight.withTimeAtStartOfDay().plusDays(1).plusHours(4),
                dateOfTheNight.withTimeAtStartOfDay().plusDays(1).plusHours(12));

        final ImmutableList<AmplitudeData> fallAsleepPeriodData = cutBefore4am.process(smoothedData);
        final ImmutableList<AmplitudeData> wakeUpPeriodData = cutAfter4am.process(smoothedData);

        // Step 2.1: Make the data more contradictive.
        final ImmutableList<AmplitudeData> sharpenFallAsleepPeriodData = NumericalUtils.zeroDataUnderAverage(fallAsleepPeriodData);
        final ImmutableList<AmplitudeData> sharpenWakeUpPeriodData = NumericalUtils.zeroDataUnderAverage(wakeUpPeriodData);

        // Step 3: Detect fall asleep period and wake up period.
        final ImmutableList<SleepThreshold> fallAsleepThresholds = SleepThreshold.generateEqualBinThresholds(sharpenFallAsleepPeriodData, 1000);
        final ImmutableList<SleepThreshold> awakeThresholds = SleepThreshold.generateEqualBinThresholds(sharpenWakeUpPeriodData, 1000);

        // Step 3.1: Select thresholds for awake detections
        final SleepThreshold fallAsleepThreshold = selectThresholdOnAwakeSegments(sharpenFallAsleepPeriodData, fallAsleepThresholds, PaddingMode.PAD_LATE);
        final SleepThreshold wakeUpThreshold = selectThresholdOnAwakeSegments(sharpenWakeUpPeriodData, awakeThresholds, PaddingMode.PAD_EARLY);


        //SleepThreshold selectedThreshold = SleepThresholdSelector.selectAverage(buffer);
        final Segment sleepSegment = new Segment();
        sleepSegment.setStartTimestamp(rawData.get(0).timestamp);
        sleepSegment.setEndTimestamp(rawData.get(rawData.size() - 1).timestamp);


        if(fallAsleepThreshold != null) {
            // Step 3.2: Generate fall asleep period based on selected threshold.
            final Segment beforeSleepAwakeSegment = getAwakeSegment(sharpenFallAsleepPeriodData, fallAsleepThreshold, PaddingMode.PAD_LATE);
            if(beforeSleepAwakeSegment != null) {
                // Set the start of sleeping period as the end of awake period before sleep.
                sleepSegment.setStartTimestamp(beforeSleepAwakeSegment.getEndTimestamp());
            }
        }

        if(wakeUpThreshold != null) {
            // Step 3.3: Generate wake-up period by selected threshold.
            final Segment wakeUpSegment = getAwakeSegment(sharpenWakeUpPeriodData, wakeUpThreshold, PaddingMode.PAD_EARLY);
            if(wakeUpSegment != null) {
                // Set teh end of sleeping period as the start of wake-up period.
                sleepSegment.setEndTimestamp(wakeUpSegment.getStartTimestamp());
            }
        }

        sleepSegment.setOffsetMillis(rawData.get(rawData.size() - 1).offsetMillis);

        return sleepSegment;
    }

    private Segment getAwakeSegment(final List<AmplitudeData> data, final SleepThreshold threshold, final PaddingMode paddingMode){

        final LinkedList<Segment> segments = new LinkedList<Segment>();

        long startTime = data.get(0).timestamp;
        long endTime = data.get(data.size() - 1).timestamp;
        long lastTime = -1;
        SleepState state = SleepState.START;
        SleepState lastState = state;

        for(final AmplitudeData datum:data){
            if (datum.amplitude < threshold.getValue()){
                state = SleepState.SLEEP;
                if(lastState == SleepState.AWAKE){
                    endTime = datum.timestamp;

                    if(startTime > 0){
                        final Segment segment = new Segment();
                        segment.setStartTimestamp(startTime);
                        segment.setEndTimestamp(endTime);

                        if(segments.size() > 0){
                            final Segment previousSegment = segments.getLast();

                            if(startTime - previousSegment.getEndTimestamp() <= 2 * getSmoothWindow()) {
                                previousSegment.setEndTimestamp(endTime);
                            }else{
                                segments.add(segment);
                            }
                        }else {
                            segments.add(segment);
                        }

                    }

                    startTime = 0;
                    endTime = 0;
                }

            }else{
                state = SleepState.AWAKE;
                if(lastState == SleepState.SLEEP){
                    startTime = datum.timestamp;
                }
            }

            lastState = state;
            lastTime = datum.timestamp;
        }

        final LinkedList<Segment> longEnoughSegments = new LinkedList<Segment>();

        //if(segments.size() == 0){
        if(lastState == SleepState.AWAKE){
            endTime = lastTime;
            if(startTime > 0) {
                Segment segment = new Segment();
                segment.setStartTimestamp(startTime);
                segment.setEndTimestamp(endTime);
                segments.add(segment);
            }
        }
        //}

        if(segments.size() == 0){
            return null;
        }


        final Segment[] sleepSegments = segments.toArray(new Segment[0]);
        Arrays.sort(sleepSegments, new Comparator<Segment>() {
            @Override
            public int compare(Segment lhs, Segment rhs) {
                if (lhs.getDuration() == rhs.getDuration()) {
                    return 0;
                } else if (lhs.getDuration() > rhs.getDuration()) {
                    return 1;
                }

                return -1;
            }
        });

        for(int i = sleepSegments.length - 1; i >= 0; i--) {
            final Segment segment = sleepSegments[i];
            if(getSmoothWindow() >= 20 * 60 * 1000){
                if (segment.getDuration() >= 1 * getSmoothWindow()) {
                    longEnoughSegments.add(segment);
                }
            }else {
                if (segment.getDuration() >= 2 * getSmoothWindow()) {
                    //return segment;
                    longEnoughSegments.add(segment);
                }
            }
        }

        if(longEnoughSegments.size() == 0){
            return null;
        }


        final Segment[] sortedLongEnoughSegments = longEnoughSegments.toArray(new Segment[0]);
        Arrays.sort(sortedLongEnoughSegments, new Comparator<Segment>() {
            @Override
            public int compare(Segment lhs, Segment rhs) {
                if (lhs.getStartTimestamp() == rhs.getStartTimestamp()) {
                    return 0;
                } else if (lhs.getStartTimestamp() > rhs.getStartTimestamp()) {
                    return 1;
                }

                return -1;
            }
        });

        if(paddingMode == PaddingMode.PAD_LATE) {
            return sortedLongEnoughSegments[sortedLongEnoughSegments.length - 1];
        }else{
            return sortedLongEnoughSegments[0];
        }

    }



    private SleepThreshold selectThresholdOnAwakeSegments(final List<AmplitudeData> data,
                                                            final List<SleepThreshold> thresholds,
                                                            final PaddingMode paddingMode){

        final LinkedList<ThresholdRankFactor> errors = new LinkedList<ThresholdRankFactor>();
        for(final SleepThreshold threshold:thresholds){
            final Segment segment = getAwakeSegment(data, threshold, paddingMode);
            if(segment == null){
                continue;
            }

            double errorDown = 0.0;
            double errorUp = 0.0;

            for(final AmplitudeData datum:data){
                if(datum.timestamp < segment.getStartTimestamp() || datum.timestamp > segment.getEndTimestamp()){
                    if(datum.amplitude > threshold.getValue()){
                        errorDown += datum.amplitude - threshold.getValue();
                    }
                }

                if(datum.timestamp >= segment.getStartTimestamp() && datum.timestamp <= segment.getEndTimestamp()){
                    if(datum.amplitude < threshold.getValue()){
                        errorUp += threshold.getValue() - datum.amplitude;
                    }
                }
            }

            final ThresholdRankFactor errorDiff = new ThresholdRankFactor(Math.abs(errorDown - errorUp), threshold);
            errors.add(errorDiff);

        }

        final ThresholdRankFactor[] errorDiffs = errors.toArray(new ThresholdRankFactor[0]);
        Arrays.sort(errorDiffs, new Comparator<ThresholdRankFactor>() {
            @Override
            public int compare(ThresholdRankFactor lhs, ThresholdRankFactor rhs) {
                return Double.compare(lhs.errorDiff, rhs.errorDiff);
            }
        });

        if(errorDiffs.length > 0){
            return errorDiffs[0].threshold;
        }

        return null;
    }
}

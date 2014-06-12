package com.hello.suripu.algorithm.sleepdetection;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.MaxAmplitudeAggregator;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 6/11/14.
 */
public class QuietPeriodDetectionAlgorithm implements SleepDetectionAlgorithm {

    private final int smoothWindowSizeMillis;

    public QuietPeriodDetectionAlgorithm(final int smoothWindowSizeMillis){
        this.smoothWindowSizeMillis = smoothWindowSizeMillis;
    }

    @Override
    public Segment getSleepPeriod(final DataSource<AmplitudeData> dataSource, final DateTime dateOfTheNight) throws AlgorithmException {

        final ImmutableList<AmplitudeData> rawData = dataSource.getDataForDate(dateOfTheNight);
        if(rawData.size() == 0){
            throw new AlgorithmException("No data available for date: " + dateOfTheNight);
        }


        final AmplitudeDataPreprocessor smoother = new MaxAmplitudeAggregator(this.smoothWindowSizeMillis);
        final ImmutableList<AmplitudeData> smoothedData = smoother.process(rawData);
        final ImmutableList<AmplitudeData> data = NumericalUtils.roofDataByAverage(smoothedData);



        final int binNumber = 1000;
        final ImmutableList<SleepThreshold> thresholds = SleepThreshold.generateEqualBinThresholds(data, binNumber);
        if(thresholds.size() == 0){
            throw new AlgorithmException("Cannot generate threshold in " + binNumber + " bins.");
        }

        final SleepThreshold selectedThreshold = selectThresholdOnQuietPeriod(data, thresholds);
        final Segment theWholeDay = new Segment();
        theWholeDay.setStartTimestamp(data.get(0).timestamp);
        theWholeDay.setEndTimestamp(data.get(data.size() - 1).timestamp);
        theWholeDay.setOffsetMillis(data.get(0).offsetMillis);


        if(selectedThreshold == null){  // When there are too much disruptions
            return theWholeDay;
        }

        final Segment sleepSegment = getQuietPeriod(data, selectedThreshold);
        if(sleepSegment == null){
            return theWholeDay;
        }

        return sleepSegment;
    }


    private Segment getQuietPeriod(final List<AmplitudeData> data, final SleepThreshold threshold){
        final LinkedList<Segment> segments = new LinkedList<Segment>();

        long startTime = 0;
        long endTime = 0;
        long lastTimestamp = 0;
        SleepState state = SleepState.START;
        SleepState lastState = state;

        for(final AmplitudeData datum:data){
            if (datum.amplitude < threshold.getValue()){
                state = SleepState.SLEEP;
                if(lastState == SleepState.AWAKE){
                    startTime = lastTimestamp;
                }

            }else{
                state = SleepState.AWAKE;
                if(lastState == SleepState.SLEEP){
                    endTime = datum.timestamp;

                    if(startTime < endTime && startTime > 0){
                        final Segment segment = new Segment();
                        segment.setStartTimestamp(startTime);
                        segment.setEndTimestamp(endTime);

                        if(segments.size() > 0){
                            Segment previousSegment = segments.getLast();
                            if(startTime - previousSegment.getEndTimestamp() <= 3 * this.smoothWindowSizeMillis) {
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
            }

            lastState = state;
            lastTimestamp = datum.timestamp;
        }

        if(segments.size() > 0) {
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

            if (sleepSegments.length > 0) {
                final Segment segment = sleepSegments[sleepSegments.length - 1];
                if(segment.getDuration() > 5 * 60 * 60 * 1000){
                    return segment;
                }

            }
        }

        //Log.i("END", "========" + threshold.getValue() + "==========");
        return null;

    }

    private SleepThreshold selectThresholdOnQuietPeriod(final List<AmplitudeData> data, final List<SleepThreshold> thresholds){
        final ArrayList<RankFactor> errors = new ArrayList<RankFactor>();
        for(SleepThreshold threshold:thresholds){
            final Segment segment = getQuietPeriod(data, threshold);
            if(segment == null){
                continue;
            }

            double errorUp = 0.0;
            double errorSleep = 0.0;

            for(final AmplitudeData datum:data){
                if(datum.timestamp < segment.getStartTimestamp() || datum.timestamp > segment.getEndTimestamp()){
                    if(datum.amplitude < threshold.getValue()){
                        errorUp += threshold.getValue() - datum.amplitude;
                    }
                }

                if(datum.timestamp >= segment.getStartTimestamp() && datum.timestamp <= segment.getEndTimestamp()){
                    if(datum.amplitude > threshold.getValue()){
                        errorSleep += datum.amplitude - threshold.getValue();
                    }
                }
            }

            //if(errorUp > 0.0 && errorSleep > 0.0){
            final RankFactor errorDiff = new RankFactor(Math.abs(errorSleep - errorUp), threshold);
            errors.add(errorDiff);
            //}

        }

        final RankFactor[] errorDiffs = errors.toArray(new RankFactor[0]);
        Arrays.sort(errorDiffs, new Comparator<RankFactor>() {
            @Override
            public int compare(RankFactor lhs, RankFactor rhs) {
                return Double.compare(lhs.errorDiff, rhs.errorDiff);
            }
        });

        if(errorDiffs.length > 0){
            return errorDiffs[0].threshold;
        }

        return null;
    }
}

package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.CSVFixtureTest;
import com.hello.suripu.algorithm.DateTimeTestUtils;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/19/15.
 */
public class MotionClusterTest extends CSVFixtureTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(MotionClusterTest.class);

    //@Test
    public void testGetSleepTimeSpan(){
        final List<AmplitudeData> input = loadAmpFromResource("fixtures/cl_motion_2015_03_12_gap_filled.csv");
        final List<AmplitudeData> kickoffs = loadKickOffFromResource("fixtures/cl_motion_2015_03_12_gap_filled.csv");
        final int timezoneOffset = input.get(0).offsetMillis;
        final MotionCluster cluster = MotionCluster.create(input, kickoffs, 100000, false, false);
        final Segment sleepPeriod =  cluster.getSleepTimeSpan();

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepPeriod.getStartTimestamp(), timezoneOffset),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 03:08:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepPeriod.getEndTimestamp(), timezoneOffset),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 11:12:00")));
    }

    //@Test
    public void testTrimBySleepPeriod(){
        final List<AmplitudeData> input = loadAmpFromResource("fixtures/cl_motion_2015_03_12_gap_filled.csv");
        final List<AmplitudeData> kickoffs = loadKickOffFromResource("fixtures/cl_motion_2015_03_12_gap_filled.csv");
        final List<AmplitudeData> actualFeature = MotionCluster.getInputFeatureFromMotions(input).get(MotionFeatures.FeatureType.MOTION_COUNT_20MIN);

        final int timezoneOffset = input.get(0).offsetMillis;
        final MotionCluster cluster = MotionCluster.create(input, kickoffs, 100000, false, false);
        final Segment sleepPeriod =  cluster.getSleepTimeSpan();
        final List<AmplitudeData> trimmed = MotionCluster.trim(actualFeature, sleepPeriod.getStartTimestamp(), sleepPeriod.getEndTimestamp());
        assertThat(DateTimeTestUtils.millisToLocalUTC(trimmed.get(0).timestamp, timezoneOffset),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 03:08:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepPeriod.getEndTimestamp(), timezoneOffset),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 11:12:00")));
    }


    @Test
    public void testTrim(){
        final List<AmplitudeData> original = new ArrayList<>();
        final List<AmplitudeData> expected = new ArrayList<>();

        final DateTime now = DateTime.now();
        final DateTime trimTime = now.plusMinutes(11);
        for(int i = 0; i < 60; i++){
            original.add(new AmplitudeData(now.plusMinutes(i).getMillis(), i, 0));
            if(!now.plusMinutes(i).isBefore(trimTime)){
                expected.add(original.get(original.size() - 1));
            }
        }

        final List<AmplitudeData> actual = MotionCluster.trim(original, trimTime.getMillis(), original.get(original.size() - 1).timestamp);
        assertThat(actual.size(), is(expected.size()));

        for(int i = 0; i < actual.size(); i++){
            assertThat(actual.get(i).timestamp, is(expected.get(i).timestamp));
            assertThat(actual.get(i).offsetMillis, is(expected.get(i).offsetMillis));
            assertThat(actual.get(i).amplitude, is(actual.get(i).amplitude));
        }
    }

    @Test
    public void testGetThreshold(){
        final List<AmplitudeData> input = loadAmpFromResource("fixtures/cl_motion_2015_03_12_gap_filled.csv");
        final List<AmplitudeData> expectedFeature = loadAmpFromResource("fixtures/cl_feature_2015_03_12.csv");
        final List<AmplitudeData> actualFeature = MotionFeatures.generateTimestampAlignedFeatures(input,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES, false)
                .get(MotionFeatures.FeatureType.MOTION_COUNT_20MIN);

        assertThat(actualFeature.size(), is(expectedFeature.size()));
        for(int i = 0; i < expectedFeature.size(); i++){
            assertThat(actualFeature.get(i).amplitude, is(expectedFeature.get(i).amplitude));
        }

        final List<AmplitudeData> features = MotionCluster.getInputFeatureFromMotions(input).get(MotionFeatures.FeatureType.MOTION_COUNT_20MIN);

        assertThat(features.size(), is(81));
        final double threshold = NumericalUtils.mean(features);

        assertThat((float)threshold, is((float)3.41975308642));
    }
}

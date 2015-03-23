package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.CSVFixtureTest;
import com.hello.suripu.algorithm.DateTimeUtils;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/19/15.
 */
public class MotionClusterTest extends CSVFixtureTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(MotionClusterTest.class);

    @Test
    public void testGetSleepTimeSpan(){
        final List<AmplitudeData> input = loadFromResource("fixtures/cl_motion_2015_03_12_gap_filled.csv");
        final int timezoneOffset = input.get(0).offsetMillis;
        final MotionCluster cluster = MotionCluster.create(input);
        final Segment sleepPeriod =  cluster.getSleepTimeSpan();

        assertThat(DateTimeUtils.millisToLocalUTC(sleepPeriod.getStartTimestamp(), timezoneOffset), is(DateTimeUtils.stringToLocalUTC("2015-03-13 03:19:00")));
        assertThat(DateTimeUtils.millisToLocalUTC(sleepPeriod.getEndTimestamp(), timezoneOffset), is(DateTimeUtils.stringToLocalUTC("2015-03-13 11:12:00")));
    }

    @Test
    public void testGetThreshold(){
        final List<AmplitudeData> input = loadFromResource("fixtures/cl_motion_2015_03_12_gap_filled.csv");
        final List<AmplitudeData> expectedFeature = loadFromResource("fixtures/cl_feature_2015_03_12.csv");
        final List<AmplitudeData> actualFeature = MotionFeatures.generateTimestampAlignedFeatures(input,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES, false)
                .get(MotionFeatures.FeatureType.MOTION_COUNT_20MIN);

        assertThat(actualFeature.size(), is(expectedFeature.size()));
        for(int i = 0; i < expectedFeature.size(); i++){
            assertThat(actualFeature.get(i).amplitude, is(expectedFeature.get(i).amplitude));
        }

        final List<AmplitudeData> features = MotionCluster.getInputFeatureFromMotions(input);

        assertThat(features.size(), is(81));
        final double threshold = NumericalUtils.mean(features);

        assertThat((float)threshold, is((float)3.41975308642));
    }
}

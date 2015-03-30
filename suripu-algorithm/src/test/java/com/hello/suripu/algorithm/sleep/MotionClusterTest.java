package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.CSVFixtureTest;
import com.hello.suripu.algorithm.DateTimeTestUtils;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.DataUtils;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import org.joda.time.DateTimeConstants;
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
        final List<AmplitudeData> input = DataUtils.makePositive(DataUtils.dedupe(loadAmpFromResource("fixtures/pang_motion_2015_03_26_raw.csv")));
        final List<AmplitudeData> kickoffs = DataUtils.dedupe(loadKickOffFromResource("fixtures/pang_motion_2015_03_26_raw.csv"));
        final double ampMean = NumericalUtils.mean(input);
        final double kickOffMean = NumericalUtils.mean(kickoffs);

        final int timezoneOffset = input.get(0).offsetMillis;
        final List<AmplitudeData> alignedAmp = DataUtils.insertEmptyData(
                DataUtils.fillMissingValues(input, DateTimeConstants.MILLIS_PER_MINUTE),
                20, 1);
        final List<AmplitudeData> alignedKicks = DataUtils.insertEmptyData(
                DataUtils.fillMissingValues(kickoffs, DateTimeConstants.MILLIS_PER_MINUTE),
                20, 1);
        

        final MotionCluster cluster = MotionCluster.create(alignedAmp, ampMean, alignedKicks, kickOffMean);
        MotionCluster.printClusters(cluster.getCopyOfClusters());
        final Optional<Segment> sleepPeriodOptional =  SleepPeriod.getSleepPeriod(alignedAmp, MotionCluster.toSegments(cluster.getCopyOfClusters()));
        assertThat(sleepPeriodOptional.isPresent(), is(true));
        final Segment sleepPeriod = sleepPeriodOptional.get();

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepPeriod.getStartTimestamp(), timezoneOffset),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-27 04:00:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepPeriod.getEndTimestamp(), timezoneOffset),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-27 10:55:00")));
    }
}

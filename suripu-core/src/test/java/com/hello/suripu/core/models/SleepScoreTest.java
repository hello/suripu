package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 9/17/14.
 */
public class SleepScoreTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(SleepScoreTest.class);
    private static int OFFSET_MILLIS = -25200000;


    @Test
    public void testNonEmptyScores() {
        final Long accountID = 1L;
        final Long pillID = 10L;
        final int dateBucketPeriod = 15;
        final SortedSet<PillSample> data = new TreeSet<>();

        Random r = new Random(1);
        final PillSample s = new PillSample(pillID, DateTime.now(), r.nextFloat(), this.OFFSET_MILLIS);
        data.add(s);
        List<SleepScore> scores = SleepScore.computeSleepScore(accountID, pillID, data, dateBucketPeriod);
        assertThat(scores.isEmpty(), is(Boolean.FALSE));
    }

    @Test
    public void testNumberOfScoresCreated() {
        final Long accountID = 1L;
        final Long pillID = 10L;
        final int dateBucketPeriod = 20;

        //let's create 3 groups, 2 samples per group
        final int numGroups = 3;
        final int numSamplesPerGroup = 3;
        final SortedSet<PillSample> data = this.createSamples(pillID, numGroups, numSamplesPerGroup, dateBucketPeriod);

        List<SleepScore> scores = SleepScore.computeSleepScore(accountID, pillID, data, dateBucketPeriod);
        assertThat(scores.size(), is(numGroups));
    }

    @Test
    public void testSleepScoreDateUTC() {
        final Long accountID = 1L;
        final Long pillID = 10L;
        final int dateBucketPeriod = 10;

        //let's create 3 groups, 2 samples per group
        final int numGroups = 4;
        final int numSamplesPerGroup = 3;
        final SortedSet<PillSample> data = this.createSamples(pillID, numGroups, numSamplesPerGroup, dateBucketPeriod);

        List<SleepScore> scores = SleepScore.computeSleepScore(accountID, pillID, data, dateBucketPeriod);
        for (int i=0; i<scores.size(); i++) {
            final int bucketMinute = i * dateBucketPeriod;
            final int scoreDateUTCMinute = scores.get(i).dateBucketUTC.getMinuteOfHour();
            assertThat(scoreDateUTCMinute, is(bucketMinute));
        }
    }

    private SortedSet<PillSample> createSamples(final Long internalPillId, final int numGroups, final int numSamplesPerGroup, final int dateBucketPeriod) {
        final DateTime startTime = DateTime.now().withTimeAtStartOfDay();
        startTime.withHourOfDay(12);
        Random r = new Random(2);

        //let's create X groups, Y samples per group
        final int randomMax = (dateBucketPeriod / numSamplesPerGroup) - 1;
        final SortedSet<PillSample> data = new TreeSet<>();
        for (int i=0; i<numGroups; i++ ) {
            int minutes = i * dateBucketPeriod;
            for (int j=0; j<numSamplesPerGroup; j++) {
                minutes += r.nextInt(randomMax) + 1;
                final PillSample sample = new PillSample(internalPillId, startTime.withMinuteOfHour(minutes), r.nextFloat(), this.OFFSET_MILLIS);
                LOGGER.debug("sample {}", sample);
                data.add(sample);
            }
        }
        return data;
    }
}

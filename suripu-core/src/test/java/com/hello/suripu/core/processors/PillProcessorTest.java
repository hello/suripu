package com.hello.suripu.core.processors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.binders.BindSleepScore;
import com.hello.suripu.core.models.PillSample;
import com.hello.suripu.core.models.SleepScore;
import org.joda.time.DateTime;
import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 9/19/14.
 */
public class PillProcessorTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillProcessorTest.class);

    private static int OFFSET_MILLIS = -25200000;
    private static int CHECKPOINT_THRESHOLD = 1;

    final DateTime startTime = DateTime.now().withTimeAtStartOfDay();
    private static String pillID = "10";
    private static Long accountID = 1L;

    private final SleepScoreDAO sleepScoreDAO = new SleepScoreDAO() {
        @Override
        public long insert(@BindSleepScore SleepScore sleepScore) {
            return 1;
        }

        @Override
        public ImmutableList<SleepScore> getByAccountBetweenDateBucket(@Bind("account_id") Long account_id, @Bind("sleep_utc") DateTime sleepUTC, @Bind("awake_utc") DateTime awakeUTC) {
            return null;
        }

        @Override
        public long incrementSleepScoreByPillDateBucket(@Bind("pill_id") Long pillID, @Bind("date_bucket_utc") DateTime dateBucketUTC, @Bind("bucket_score") int bucketScore, @Bind("sleep_duration") int sleepDuration, @Bind("agitation_num") int agitationNum, @Bind("agitation_tot") long agitationTot, @Bind("updated") DateTime updated) {
            return 1;
        }
    };


    @Test
    public void checkPillForScoringPassSize() {
        LOGGER.debug("---- Testing for inserts based on data size ----");
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        Random r = new Random(5);
        final int dateMinuteBucket = 10;

        // create pill data for accountID
        for (int i=0; i < dateMinuteBucket + 1; i++) {
            final PillSample sample = new PillSample(this.pillID, this.startTime.withMinuteOfHour(i), r.nextFloat(), this.OFFSET_MILLIS);
            samples.put(this.accountID, sample);
        }

        final PillProcessor pillProcessor = new PillProcessor(this.sleepScoreDAO, dateMinuteBucket, this.CHECKPOINT_THRESHOLD);
        final boolean ok = pillProcessor.processPillRecords(samples);
        final int numToProcess = pillProcessor.getNumInserted();
        assertThat(numToProcess, is(2));
    }

    @Test
    public void checkPillForScoringPassTime() {
        LOGGER.debug("---- Testing for inserts based on Time ----");
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        Random r = new Random(5);
        final int dateMinuteBucket = 10;

        // create pill data for accountID
        for (int i=0; i < 5; i++) {
            final PillSample sample = new PillSample(this.pillID, this.startTime.withMinuteOfHour(i*5), r.nextFloat(), this.OFFSET_MILLIS);
            samples.put(this.accountID, sample);
        }

        final PillProcessor pillProcessor = new PillProcessor(this.sleepScoreDAO, dateMinuteBucket, this.CHECKPOINT_THRESHOLD);
        final boolean ok = pillProcessor.processPillRecords(samples);
        final int numToProcess = pillProcessor.getNumInserted();
        assertThat(numToProcess, is(3));
    }

    @Test
    public void checkPillForScoringZero() {
        LOGGER.debug("---- Testing for zero inserts, and pill data size ----");
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        Random r = new Random(5);
        final int dateMinuteBucket = 20;

        // create pill data for accountID
        for (int i=0; i < 5; i++) {
            final PillSample sample = new PillSample(this.pillID, startTime.withMinuteOfHour(i*2), r.nextFloat(), this.OFFSET_MILLIS);
            samples.put(this.accountID, sample);
        }

        final PillProcessor pillProcessor = new PillProcessor(this.sleepScoreDAO, dateMinuteBucket, this.CHECKPOINT_THRESHOLD);
        final boolean ok = pillProcessor.processPillRecords(samples);
        final int numToProcess = pillProcessor.getToProcessIdsCount();
        assertThat(numToProcess, is(0));

        final int numData = pillProcessor.getPillIDDataSize(this.pillID);
        assertThat(numData, is(5));
    }

    @Test
    public void checkPillForScoringUpdates() {
        LOGGER.debug("---- Testing for inserts ----");
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        Random r = new Random(5);
        final int dateMinuteBucket = 10;

        // create pill data for accountID
        for (int i=0; i < dateMinuteBucket + 2; i++) {
            final PillSample sample = new PillSample(this.pillID, startTime.withMinuteOfHour(i*3), r.nextFloat(), this.OFFSET_MILLIS);
            samples.put(this.accountID, sample);
        }

        final PillProcessor pillProcessor = new PillProcessor(this.sleepScoreDAO, dateMinuteBucket, this.CHECKPOINT_THRESHOLD);
        boolean ok = pillProcessor.processPillRecords(samples);
        final int numInserted = pillProcessor.getNumInserted();
        assertThat(numInserted, is(4));

        final int numData = pillProcessor.getPillIDDataSize(this.pillID);
        assertThat(numData, is(0));

        LOGGER.debug("---- add data to old buckets ----");
        samples.removeAll(this.accountID);

        LOGGER.debug("samples size = {}", samples.get(this.accountID).size());

        final PillSample sample1 = new PillSample(this.pillID, startTime.withMinuteOfHour(1), r.nextFloat(), this.OFFSET_MILLIS);
        samples.put(this.accountID, sample1);

        final PillSample sample2 = new PillSample(this.pillID, startTime.withMinuteOfHour(5), r.nextFloat(), this.OFFSET_MILLIS);
        samples.put(this.accountID, sample2);

        final PillSample sample3 = new PillSample(this.pillID, startTime.withMinuteOfHour(39), r.nextFloat(), this.OFFSET_MILLIS);
        samples.put(this.accountID, sample3);

        LOGGER.debug("re-added samples size = {}", samples.get(this.accountID).size());

        ok = pillProcessor.processPillRecords(samples);
        final int numInsertedAgain = pillProcessor.getNumInserted();
        assertThat(numInsertedAgain, is(6));


    }

    @Test
    public void checkPillForScoringTooOldCondition() {
        LOGGER.debug("---- Testing for too old condition ----");
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        Random r = new Random(5);
        final int dateMinuteBucket = 10;

        final PillProcessor pillProcessor = new PillProcessor(this.sleepScoreDAO, dateMinuteBucket, this.CHECKPOINT_THRESHOLD);

        final PillSample sample1 = new PillSample(this.pillID, startTime.withMinuteOfHour(1), r.nextFloat(), this.OFFSET_MILLIS);
        samples.put(this.accountID, sample1);

        final PillSample sample2 = new PillSample(this.pillID, startTime.withMinuteOfHour(5), r.nextFloat(), this.OFFSET_MILLIS);
        samples.put(this.accountID, sample2);

        boolean ok = pillProcessor.processPillRecords(samples);
        final int numToProcessed = pillProcessor.getToProcessIdsCount();
        LOGGER.debug("pill to process {}", numToProcessed);
        assertThat(numToProcessed, is(0));

        final int numData = pillProcessor.getPillIDDataSize(this.pillID);
        assertThat(numData, is(2));

        LOGGER.debug("---- add data to old buckets");
        samples.removeAll(this.accountID);
        LOGGER.debug("samples size = {}", samples.get(this.accountID).size());

        final PillSample sample3 = new PillSample(this.pillID, startTime.withMinuteOfHour(55), r.nextFloat(), this.OFFSET_MILLIS);
        samples.put(2L, sample3);

        LOGGER.debug("re-added samples size = {}", samples.get(2L).size());

        ok = pillProcessor.processPillRecords(samples);
        final int numInsertedAgain = pillProcessor.getNumInserted();
        assertThat(numInsertedAgain, is(2));


    }

}
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
public class PillScoreBatchByRecordProcessorTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreBatchByRecordProcessorTest.class);

    private static int OFFSET_MILLIS = -25200000;
    private static int CHECKPOINT_THRESHOLD = 200;

    final DateTime startTime = DateTime.now().withTimeAtStartOfDay();
    private static String pillID = "10";

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
    public void testInsufficientRecords() {
        LOGGER.debug("---- Test adding records to processor memory, will not checkpoint ----");
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        Random r = new Random(5);
        final int dateMinuteBucket = 15;
        final int numRecords = this.CHECKPOINT_THRESHOLD - 20;

        // create pill data for accountID
        for (int i=0; i < numRecords; i++) {
            final PillSample sample = new PillSample(this.pillID, this.startTime.plusSeconds(i), r.nextFloat(), this.OFFSET_MILLIS);
            samples.put(r.nextLong(), sample);
        }

        final PillScoreBatchByRecordsProcessor pillProcessor = new PillScoreBatchByRecordsProcessor(this.sleepScoreDAO, dateMinuteBucket, this.CHECKPOINT_THRESHOLD);
        final boolean ok = pillProcessor.processPillRecords(samples);
        assertThat(ok, is(false));

        final int numRecordsInProcessor = pillProcessor.getNumRecordsInMemory();
        assertThat(numRecordsInProcessor, is(numRecords));

    }

    @Test
    public void testSufficientRecords() {
        LOGGER.debug("---- Test adding records to processor memory, CHECKPOINT OK ----");
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        Random r = new Random(5);
        final int dateMinuteBucket = 15;
        final int numRecords = this.CHECKPOINT_THRESHOLD + 20;

        // create pill data for accountID
        for (int i=0; i < numRecords; i++) {
            final PillSample sample = new PillSample(this.pillID, this.startTime.plusSeconds(i), r.nextFloat(), this.OFFSET_MILLIS);
            samples.put(r.nextLong(), sample);
        }

        final PillScoreBatchByRecordsProcessor pillProcessor2 = new PillScoreBatchByRecordsProcessor(this.sleepScoreDAO, dateMinuteBucket, this.CHECKPOINT_THRESHOLD);
        final boolean ok = pillProcessor2.processPillRecords(samples);
        assertThat(ok, is(true));

        final int numRecordsInProcessor = pillProcessor2.getNumPillRecordsProcessed();
        assertThat(numRecordsInProcessor, is(numRecords));

    }

    @Test
    public void testInsufficientRecordsButPassTimeThreshold() {
        LOGGER.debug("---- Test adding insufficient records to processor memory, CHECKPOINT OK ----");
        final ListMultimap<Long, PillSample> samples = ArrayListMultimap.create();
        Random r = new Random(5);
        final int dateMinuteBucket = 5;
        final int numRecords = this.CHECKPOINT_THRESHOLD - 20;

        final PillScoreBatchByRecordsProcessor pillProcessor2 = new PillScoreBatchByRecordsProcessor(this.sleepScoreDAO, dateMinuteBucket, this.CHECKPOINT_THRESHOLD);
        PillSample sample = new PillSample(this.pillID, this.startTime, r.nextFloat(), this.OFFSET_MILLIS);
        samples.put(r.nextLong(), sample);
        boolean ok = pillProcessor2.processPillRecords(samples);
        assertThat(ok, is(false));

        // create pill data for accountID
        samples.clear();
        for (int i=0; i < numRecords; i++) {
            sample = new PillSample(this.pillID, this.startTime.plusMinutes(i*2 + 1), r.nextFloat(), this.OFFSET_MILLIS);
            samples.put(r.nextLong(), sample);
        }

        ok = pillProcessor2.processPillRecords(samples);
        assertThat(ok, is(true));

        final int numRecordsInProcessor = pillProcessor2.getNumPillRecordsProcessed();
        assertThat(numRecordsInProcessor, is(numRecords+1));

    }

}

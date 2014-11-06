package com.hello.suripu.core.processors;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.models.PillSample;
import com.hello.suripu.core.models.SleepScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Created by kingshy on 9/19/14.
 */
public class PillScoreBatchByIdProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreBatchByIdProcessor.class);

    private final SleepScoreDAO sleepScoreDAO;

    // keeping states
    private final Map<String, Long> pillAccountID; // k: pillID, v: accountID
    private final SortedSetMultimap<String, PillSample> pillData; // k: pillID
    private final Set<String> toProccessedIDs;
    private long lastRecordDTMillis = 0;

    // tracking process stats
    private int numPillsProcessed;
    private int numInserts = 0;
    private int numUpdates = 0;
    private int numScores = 0;
    private int decodeErrors = 0;

    // bunch of constants
    private final int checkpointThreshold; // no. of pills processed before we checkpoint kinesis
    private final int dateMinuteBucket; // data size threshold to process the pill
    private final int dateMinuteBucketMillis;
    private long tooOldThreshold;

    public PillScoreBatchByIdProcessor(SleepScoreDAO sleepScoreDAO, final int dateMinuteBucket, final int checkpointThreshold) {
        this.sleepScoreDAO = sleepScoreDAO;
        this.dateMinuteBucket = dateMinuteBucket;
        this.dateMinuteBucketMillis = dateMinuteBucket * 60 * 1000;
        this.tooOldThreshold = (long) this.dateMinuteBucketMillis * 2L;
        this.checkpointThreshold = checkpointThreshold;
        this.pillAccountID = new HashMap<>();
        this.pillData = TreeMultimap.create();
        this.toProccessedIDs = new HashSet<>();
        this.numPillsProcessed = 0;

    }

    /** main entry point to process any pill data
     *
     * @param pillRecords
     * @return
     */
    public boolean processPillRecords(final ListMultimap<Long, PillSample> pillRecords) {

        // add records to memory store
        final long lastTimestampMillis = this.parsePillRecords(pillRecords);

        // check all data in memory, process those that are too old
        final int added = checkAllPillData(lastTimestampMillis);

        LOGGER.debug("number of pills to process = {}, added={}", this.getToProcessIdsCount(), added);

        // compute scores and save to DB
        if (!this.toProccessedIDs.isEmpty()) {
            final int numSavedScores = this.computeAndSaveScores();

            if (numSavedScores > 0 && this.numPillsProcessed % this.checkpointThreshold == 0) {
                return true; // okay checkpoint
            }
        }
        return false;
    }

    public int getNumInserted() {
        return this.numInserts;
    }

    public int getNumUpdates() {
        return this.numUpdates;
    }
    public int getToProcessIdsCount() {
        return this.toProccessedIDs.size();
    }

    public int getPillIDDataSize(final String pillID) {
        return this.pillData.get(pillID).size();
    }

    /**
     * Add each record to in-memory maps
     * @param pillRecords
     * @return timestamp for the last record in this set
     */
    private long parsePillRecords(final ListMultimap<Long, PillSample> pillRecords) {
        long lastTimestampMillis = 0;

        for (final Long accountID : pillRecords.keySet()) {

            List<PillSample> records = pillRecords.get(accountID);
            for (final PillSample record : records) {
                final String pillID = record.sampleID;

                this.pillAccountID.put(pillID, accountID); // map pill-id to account-id
                this.pillData.put(pillID, record); // stores pill data

                if (!this.toProccessedIDs.contains(pillID)) {
                    final SortedSet<PillSample> data = this.pillData.get(pillID);
                    if (checkPillDataForScoring(data)) {
                        this.toProccessedIDs.add(pillID); // pill is ready for scoring
                    }
                }

                if (record.dateTime.getMillis() > lastTimestampMillis) {
                    lastTimestampMillis = record.dateTime.getMillis(); // track last-seen timestamp
                }

            }
        }
        return lastTimestampMillis;
    }

    /**
     * Check if a pill has accumulated enough data for scoring
     * @param samples
     * @return
     */
    private boolean checkPillDataForScoring(final SortedSet<PillSample> samples) {
        if (samples.size() > this.dateMinuteBucket) {
            return true; // sufficient samples
        }

        final PillSample firstSample = samples.first();
        final PillSample lastSample = samples.last();
        if (lastSample.dateTime.getMillis() - firstSample.dateTime.getMillis() >= this.dateMinuteBucketMillis) {
            return true; // accumulate more than one required bucket of data
        }
        return false;
    }

    /**
     * Check in-memory store for data that is too old. Mark these for scoring.
     * @param lastTimestampMillis
     * @return
     */
    private int checkAllPillData(final long lastTimestampMillis) {
        // note: pill data can arrive out of order, timestamp from data is not always increasing
        final long timestampDiff = lastTimestampMillis - this.lastRecordDTMillis;
        if (timestampDiff > 0 && timestampDiff < this.dateMinuteBucketMillis) {
            return 0;
        }

        if (timestampDiff > 0) {
            this.lastRecordDTMillis = lastTimestampMillis;
        }

        int added = 0;
        for (final String pillID : this.pillAccountID.keySet()) {
            if (!this.toProccessedIDs.contains(pillID)) {
                final PillSample lastSample = this.pillData.get(pillID).last();
                if (this.lastRecordDTMillis - lastSample.dateTime.getMillis() > this.tooOldThreshold) {
                    this.toProccessedIDs.add(pillID);
                }
            }
        }
        return added;
    }

    /**
     * Compute scores and save to DB
     * @return
     */
    private int computeAndSaveScores() {
        int processed = 0;
        Set<String> successPillIDs = new HashSet<>();

        for (final String pillID: this.toProccessedIDs) {
            final Long accountID = this.pillAccountID.get(pillID);
            final SortedSet<PillSample> data = this.pillData.get(pillID);

            if (data.isEmpty()) {
                LOGGER.error("No data for scoring {}", pillID);
                continue;
            }

            final List<SleepScore> scores = SleepScore.computeSleepScore(accountID, pillID, data, this.dateMinuteBucket);

            final Map<String, Integer> stats = this.sleepScoreDAO.saveScores(scores);

            final int saved = (stats.get("updated") + stats.get("inserted"));
            this.numUpdates += stats.get("updated");
            this.numInserts += stats.get("inserted");
            this.numScores += scores.size();
            if (saved > 0) {
                successPillIDs.add(pillID);
                processed++;
            }
        }

        for (final String pillID: successPillIDs) {
            this.pillData.removeAll(pillID);
            this.pillAccountID.remove(pillID);
            this.toProccessedIDs.remove(pillID);
        }

        this.numPillsProcessed += processed;
        return processed;
    }
}

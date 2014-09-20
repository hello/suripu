package com.hello.suripu.core.processors;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.models.SensorSample;
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
public class PillProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillProcessor.class);

    private final SleepScoreDAO sleepScoreDAO;

    // keeping states
    private final Map<String, Long> pillAccountID; // k: pillID, v: accountID
    private final SortedSetMultimap<String, SensorSample> pillData; // k: pillID
    private final Set<String> toProccessedIDs;
    private long lastRecordDTMillis = 0;

    // tracking process stats
    private int numPillsProcessed;
    private int numInserts = 0;
    private int numUpdates = 0;
    private int numScores = 0;
    private int decodeErrors = 0;

    // bunch of constants
    private int checkpointThreshold = 1; // no. of pills processed before we checkpoint kinesis
    private int dateMinuteBucket; // data size threshold to process the pill
    private int dateMinuteBucketMillis;
    private long tooOldThreshold;

    public PillProcessor(SleepScoreDAO sleepScoreDAO, final int dateMinuteBucket, final int checkpointThreshold) {
        this.sleepScoreDAO = sleepScoreDAO;
        this.dateMinuteBucket = dateMinuteBucket;
        this.dateMinuteBucketMillis = dateMinuteBucket * 1000;
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
    public boolean processPillRecords(final ListMultimap<Long, SensorSample> pillRecords) {

        // add records to memory store
        final long lastTimestampMillis = this.parsePillRecords(pillRecords);

        // check all data in memory, process those that are too old
        final int added = checkAllPillData(lastTimestampMillis);

        // compute scores and save to DB
        final int numSavedScores = this.computeAndSaveScores();

        if (numSavedScores > 0 && this.numPillsProcessed % this.checkpointThreshold == 0) {
            return true; // okay checkpoint
        }
        return false;
    }

    /**
     * Add each record to in-memory maps
     * @param pillRecords
     * @return timestamp for the last record in this set
     */
    private long parsePillRecords(final ListMultimap<Long, SensorSample> pillRecords) {
        long lastTimestampMillis = 0;
        for (final Long accountID : pillRecords.keySet()) {
            List<SensorSample> records = pillRecords.get(accountID);

            for (final SensorSample record : records) {
                final String pillID = record.id;
                this.pillAccountID.put(pillID, accountID);
                this.pillData.put(pillID, record);

                if (!this.toProccessedIDs.contains(pillID)) {
                    final SortedSet<SensorSample> data = this.pillData.get(pillID);
                    if (checkPillDataForScoring(data)) {
                        this.toProccessedIDs.add(pillID);
                    }
                }

                if (record.dateTime.getMillis() > lastTimestampMillis) {
                    lastTimestampMillis = record.dateTime.getMillis();
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
    private boolean checkPillDataForScoring(final SortedSet<SensorSample> samples) {
        if (samples.size() > this.dateMinuteBucket) {
            return true;
        }

        SensorSample firstSample = samples.first();
        SensorSample lastSample = samples.last();
        if (lastSample.dateTime.getMillis() - firstSample.dateTime.getMillis() >= this.dateMinuteBucketMillis) {
            return true;
        }
        return false;
    }

    /**
     * Check in-memory store for data that is too old. Mark these for scoring.
     * @param lastTimestampMillis
     * @return
     */
    private int checkAllPillData(final long lastTimestampMillis) {
        if (lastTimestampMillis - this.lastRecordDTMillis < this.dateMinuteBucketMillis) {
            return 0;
        }

        int added = 0;
        for (final String pillID : this.pillAccountID.keySet()) {
            if (!this.toProccessedIDs.contains(pillID)) {
                final SensorSample lastSample = this.pillData.get(pillID).last();
                if (lastTimestampMillis - lastSample.dateTime.getMillis() > tooOldThreshold) {
                    this.toProccessedIDs.add(pillID);
                }
            }
        }
        this.lastRecordDTMillis = lastTimestampMillis;
        return added;
    }

    /**
     * Compute scores and save to DB
     * @return
     */
    private int computeAndSaveScores() {
        int processed = 0;
        for (String pillID: this.toProccessedIDs) {
            final Long accountID = this.pillAccountID.get(pillID);
            final SortedSet<SensorSample> data = this.pillData.get(pillID);

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
                this.numPillsProcessed++;
                this.pillData.removeAll(pillID);
                this.pillAccountID.remove(pillID);
                this.toProccessedIDs.remove(pillID);
                processed++;
            }
        }
        return processed;
    }
}

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
public class PillScoreBatchByRecordsProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreBatchByRecordsProcessor.class);

    private final SleepScoreDAO sleepScoreDAO;

    // keeping states
    private final Map<String, Long> pillAccountID; // k: pillID, v: accountID
    private final SortedSetMultimap<String, PillSample> pillData; // k: pillID
    private long lastProcessedTimestampMillis;

    // tracking process stats
    private int numRecordsInMemory;
    private int numPillRecordsProcessed;
    private int numInserts = 0;
    private int numUpdates = 0;

    // bunch of constants
    private final int checkpointThreshold; // no. of pills processed before we checkpoint kinesis
    private final int dateMinuteBucket; // data size threshold to process the pill
    private final long checkpointTimeThreshold;

    public PillScoreBatchByRecordsProcessor(SleepScoreDAO sleepScoreDAO, final int dateMinuteBucket, final int checkpointThreshold) {
        this.sleepScoreDAO = sleepScoreDAO;
        this.dateMinuteBucket = dateMinuteBucket;
        this.checkpointThreshold = checkpointThreshold;
        this.checkpointTimeThreshold = dateMinuteBucket * 60 * 1000L; // in millis
        this.pillAccountID = new HashMap<>();
        this.pillData = TreeMultimap.create();
        this.numPillRecordsProcessed = 0;

        this.numRecordsInMemory = 0;
        this.lastProcessedTimestampMillis = 0L;

    }

    /** main entry point to process any pill data
     *
     * @param pillRecords
     * @return
     */
    public boolean processPillRecords(final ListMultimap<Long, PillSample> pillRecords) {

        // add records to memory store
        final long lastTimestampMillis = this.parsePillRecords(pillRecords);

        LOGGER.debug("number of records in memory: {}", this.numRecordsInMemory);

        // compute scores and save to DB
        if (this.lastProcessedTimestampMillis == 0) {
            this.lastProcessedTimestampMillis = lastTimestampMillis;
        }

        final long timestampDiff = lastTimestampMillis - this.lastProcessedTimestampMillis;

        // process when we have stored a number of pill records, or the process time reached a threshold
        if (this.numRecordsInMemory >= this.checkpointThreshold || timestampDiff >= this.checkpointTimeThreshold) {
            final int numSavedScores = this.computeAndSaveScores();

            if (numSavedScores == this.numRecordsInMemory) {
                this.numRecordsInMemory = 0;
                this.lastProcessedTimestampMillis = lastTimestampMillis;
                return true; // okay checkpoint
            }
        }
        return false;
    }

    public int getNumRecordsInMemory() {return this.numRecordsInMemory;}

    public int getNumPillRecordsProcessed() {return this.numPillRecordsProcessed;}
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

                if (record.dateTime.getMillis() > lastTimestampMillis) {
                    lastTimestampMillis = record.dateTime.getMillis(); // track last-seen timestamp
                }

            }
        }
        this.numRecordsInMemory = this.pillData.size();
        return lastTimestampMillis;
    }

    /**
     * Compute scores and save to DB
     * @return
     */
    private int computeAndSaveScores() {
        int processed = 0;
        Set<String> successPillIDs = new HashSet<>();

        //for (final String pillID: this.toProccessedIDs) {
        for (final String pillID : this.pillData.keySet()) {
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
            if (saved > 0) {
                successPillIDs.add(pillID);
                processed += data.size();
            }
        }

        for (final String pillID: successPillIDs) {
            this.pillData.removeAll(pillID);
            this.pillAccountID.remove(pillID);
        }

        this.numPillRecordsProcessed += processed;
        return processed;
    }
}

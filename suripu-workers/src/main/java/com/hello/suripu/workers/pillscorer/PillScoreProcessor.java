package com.hello.suripu.workers.pillscorer;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.models.SensorSample;
import com.hello.suripu.core.models.SleepScore;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class PillScoreProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreProcessor.class);


    private final SleepScoreDAO sleepScoreDAO;

    // keeping states
    private final Map<String, Long> pillAccountID;
    private final SortedSetMultimap<String, SensorSample> pillData;

    // tracking process stats
    private int numPillsProcessed;
    private int numInserts = 0;
    private int numUpdates = 0;
    private int numScores = 0;

    // bunch of constants
    private int checkpointThreshold = 1; // no. of pills processed before we checkpoint kinesis
    private int dateMinuteBucket; // data size threshold to process the pill
    private int dateMinuteBucketMillis;

    public PillScoreProcessor(final SleepScoreDAO sleepScoreDAO, final int dateMinuteBucket, final int checkpointThreshold) {
        this.sleepScoreDAO = sleepScoreDAO;
        this.dateMinuteBucket = dateMinuteBucket;
        this.dateMinuteBucketMillis = dateMinuteBucket * 1000;
        this.checkpointThreshold = checkpointThreshold;
        this.pillAccountID = new HashMap<>();
        this.pillData = TreeMultimap.create();
        this.numPillsProcessed = 0;

    }

    @Override
    public void initialize(String s) {

    }

    @Override
    @Timed
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        final Set<String> toProcessPillIds = new HashSet<String>();

        for(final Record record : records) {
            try {
                final InputProtos.PillDataKinesis data = InputProtos.PillDataKinesis.parseFrom(record.getData().array());

                // only keep instances where value != -1 to save space
                if (data.getValue() < 0) {
                    continue;
                }

                final Long accountID = Long.parseLong(data.getAccountId());
                final String pillID = data.getPillId();

                this.pillAccountID.put(pillID, accountID);

                final DateTime sampleDT = new DateTime(data.getTimestamp(), DateTimeZone.UTC);
                final DateTime dateHourMinUTC = sampleDT.withSecondOfMinute(0);
                final SensorSample sample = new SensorSample(dateHourMinUTC, data.getValue(), data.getOffsetMillis());
                this.pillData.put(pillID, sample);

                // check if we want to process the account-pillID pair
                if (!toProcessPillIds.contains(pillID)) {
                    if (this.pillData.get(pillID).size() >= this.dateMinuteBucket) {
                        toProcessPillIds.add(pillID);
                    } else {
                        // first stored datetime and current datetime exceeded threshold
                        final SensorSample firstData = this.pillData.get(pillID).first();
                        if (dateHourMinUTC.getMillis() - firstData.dateTime.getMillis() >= this.dateMinuteBucketMillis) {
                            toProcessPillIds.add(pillID);
                        }
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
                // TODO: increment error counter somewhere
            }
        }

        // TODO: what happens when we don't have 15 mins of data, say pill died
        // need to iterate through all, and check timestamp, do this every 15 mins or so.

        // Process pills with enough data
        this.processPills(toProcessPillIds);

        // checkpoint every time we have processed some numbers of pills
        if (toProcessPillIds.size() > 0 && this.numPillsProcessed % this.checkpointThreshold == 0) {
            LOGGER.debug("Checkpoint {}", this.numPillsProcessed);
            try {
                iRecordProcessorCheckpointer.checkpoint();
            } catch (InvalidStateException e) {
                LOGGER.error("checkpoint {}", e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
            }
        }
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }

    private void processPills(final Set<String> pills) {
        // processed pills with enough data
        LOGGER.debug("Number of pills to process each round {}", pills.size());
        for (String pillID : pills) {
            LOGGER.debug("Processing Pill data for ID {}", pillID);
            LOGGER.debug("Amount of data for this pill {}, {}", pillID, this.pillData.get(pillID).size());

            final Long accountID = this.pillAccountID.get(pillID);
            SortedSet<SensorSample> pillData = this.pillData.get(pillID);
            List<SleepScore> scores = SleepScore.computeSleepScore(accountID, pillID, pillData, this.dateMinuteBucket);

            final Map<String, Integer> stats = this.sleepScoreDAO.saveScores(scores);

            final int saved = (stats.get("updated") + stats.get("inserted"));
            this.numUpdates += stats.get("updated");
            this.numInserts += stats.get("inserted");
            this.numScores += scores.size();
            if (saved > 0) {
                this.numPillsProcessed++;
                this.pillData.removeAll(pillID);
            }
        }

        LOGGER.debug("Summary: Scores: {}, Inserts: {}, Updates: {}",
                this.numScores, this.numInserts, this.numUpdates);


    }

}
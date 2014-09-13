package com.hello.suripu.workers.pillscorer;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class PillScoreProcessor implements IRecordProcessor {

    private final static Logger LOGGER = (Logger) LoggerFactory.getLogger(PillScoreProcessor.class);

    private final SleepScoreDAO sleepScoreDAO;
    private int processThreshold; // process data every this number of records
    private int processThresholdMillis;

    // keeping states
    private final Map<String, String> pillAccountID;
    private final SortedSetMultimap<String, SensorSample> pillData;
    private final ListMultimap<String, String> accountSequenceNumber;
    private int numPillsProcessed;


    private static int MAX_PILLS_PROCESSED = 1;
    private static final String EMPTY_STRING = "";

    public PillScoreProcessor(final SleepScoreDAO sleepScoreDAO, final int processThreshold) {
        this.sleepScoreDAO = sleepScoreDAO;
        this.processThreshold = processThreshold;
        this.processThresholdMillis = processThreshold * 1000;
        this.pillAccountID = new HashMap<>();
        this.pillData = TreeMultimap.create();
        this.accountSequenceNumber = ArrayListMultimap.create();
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

                final String accountID = data.getAccountId();
                final String pillID = data.getPillId();

                this.pillAccountID.put(pillID, accountID);

                final DateTime sampleDT = new DateTime(data.getTimestamp(), DateTimeZone.UTC);
                final DateTime dateHourMinUTC = sampleDT.withSecondOfMinute(0);
                final SensorSample sample = new SensorSample(dateHourMinUTC, data.getValue(), data.getOffsetMillis());
                this.pillData.put(pillID, sample);

                this.accountSequenceNumber.put(accountID, record.getSequenceNumber());

                // check if we want to process the account-pillID pair
                if (this.pillData.get(pillID).size() >= this.processThreshold) {
                    LOGGER.debug("Amount of data for this pill {}, {}", pillID, this.pillData.get(pillID).size());
                    toProcessPillIds.add(pillID);
                } else {
                    // first stored datetime and current datetime exceeded threshold
                    final SensorSample firstData = this.pillData.get(pillID).first();
                    if (dateHourMinUTC.getMillis() - firstData.dateTime.getMillis() >= this.processThresholdMillis) {
                        toProcessPillIds.add(pillID);
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
                // TODO: increment error counter somewhere
            }
        }

        // what happens when we don't have 15 mins of data, say pill died
        // need to iterate through all, and check timestamp, do this every 15 mins or so.
        LOGGER.debug("Number of pills to process each round {}", toProcessPillIds.size());
        for (String pillID : toProcessPillIds) {
            LOGGER.debug("Processing Pill data for ID {}", pillID);
            List<SleepScore> scores = this.computeSleepScore(this.pillAccountID.get(pillID), pillID, this.pillData.get(pillID));
            final int saved = this.saveScore(scores);
            if (saved > 0) {
                this.numPillsProcessed++;
                this.pillData.removeAll(pillID);
            }
        }

        if (toProcessPillIds.size() > 0 && this.numPillsProcessed % this.MAX_PILLS_PROCESSED == 0) {
            // TODO: checkpoint every time we have processed some numbers of pills
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

    private List<SleepScore> computeSleepScore(String accountID, String pillID, SortedSet<SensorSample> pillData) {

        final List<SleepScore> sleepScores = new ArrayList<>();

        final SensorSample firstData = pillData.first();
        final int timeZoneOffset = firstData.timeZoneOffset;

        int agitationNum = 0;
        float agitationTot = 0;
        int duration = 0;
        int minute = (int) firstData.dateTime.getMinuteOfHour()/this.processThreshold;
        DateTime lastBucket = firstData.dateTime.withMinuteOfHour(minute * this.processThreshold);
        LOGGER.debug("======= Computing scores for this pill {}, {}", pillID, accountID);

        for (final SensorSample data: pillData) {
            minute = (int) data.dateTime.getMinuteOfHour() / this.processThreshold;
            final DateTime bucket = data.dateTime.withMinuteOfHour(minute * this.processThreshold);
            if (bucket.compareTo(lastBucket) != 0) {
                final int score = (int) (((double) agitationNum)/((double) this.processThreshold) * 100.0);
                SleepScore sleepScore = new SleepScore(0L,
                        Long.parseLong(accountID),
                        lastBucket,
                        Long.parseLong(pillID),
                        duration,
                        score,
                        false, // no customized score for now
                        this.EMPTY_STRING, // no SAX yet
                        agitationNum,
                        (long) agitationTot,
                        DateTime.now(),
                        timeZoneOffset
                );
                LOGGER.debug("created new score object for {}, {}, {}, {}", lastBucket, score, agitationNum, agitationTot);
                sleepScores.add(sleepScore);
                agitationNum = 0;
                agitationTot = 0;
                duration = 0;
                lastBucket = bucket;
            }

            LOGGER.debug("Sensor Sample {}", data.toString());
            final float value = data.val;
            if (value != -1) {
                agitationNum++;
                agitationTot = agitationTot + value;
            }
            duration++;
        }

        if (duration != 0) {
            final int score = (int) (((double) agitationNum)/((double) this.processThreshold) * 100.0);
            SleepScore sleepScore = new SleepScore(0L,
                    Long.parseLong(accountID),
                    lastBucket,
                    Long.parseLong(pillID),
                    duration,
                    score,
                    false, // no customized score for now
                    this.EMPTY_STRING, // no SAX yet
                    agitationNum,
                    (long) agitationTot,
                    DateTime.now(),
                    timeZoneOffset
            );
            LOGGER.debug("created new score object for {}, {}, {}, {}", lastBucket, score, agitationNum, agitationTot);
            sleepScores.add(sleepScore);

        }
        return sleepScores;
    }

    private int saveScore(List<SleepScore> scores) {
        int saved = 0;
        for (final SleepScore score : scores) {
            LOGGER.debug("Saving Score: account {}, datetime {}, val {}", score.accountId, score.dateHourUTC.toString(), score.timeZoneOffset);

            // this is expensive, TODO: refactor
            Optional<SleepScore> sleepScoreOptional = this.sleepScoreDAO.getByAccountAndDateHour
                    (score.accountId, score.dateHourUTC, score.timeZoneOffset);

            if (sleepScoreOptional.isPresent()) {
                final SleepScore existingScore = sleepScoreOptional.get();
                LOGGER.debug("Score exist, updating {} with {}", existingScore.totalHourScore, score.totalHourScore);
                final long updated = this.sleepScoreDAO.updateBySleepScoreId(sleepScoreOptional.get().id,
                        score.totalHourScore + existingScore.totalHourScore,
                        score.sleepDuration + existingScore.sleepDuration,
                        score.agitationNum + existingScore.agitationNum,
                        score.agitationTot + existingScore.agitationTot
                        );
                if (updated > 0) {
                    saved++;
                }
            } else {
                LOGGER.debug("Insert new score {}", score.totalHourScore);
                final long inserted = this.sleepScoreDAO.insert(score.accountId, score.dateHourUTC, score.pillID,
                        score.timeZoneOffset, score.sleepDuration, score.custom,
                        score.totalHourScore, score.saxSymbols,
                        score.agitationNum, score.agitationTot,
                        score.updated);
                if (inserted > 0) {
                    saved++;
                }
            }
        }
        return saved;
    }
}

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
                LOGGER.debug("Valid pill data for {}", accountID);

                this.pillAccountID.put(pillID, accountID);

                final DateTime sampleDT = new DateTime(data.getTimestamp(), DateTimeZone.UTC);
                final DateTime dateHourMinUTC = sampleDT.withSecondOfMinute(0);
                final SensorSample sample = new SensorSample(dateHourMinUTC, data.getValue(), data.getOffsetMillis());
                this.pillData.put(pillID, sample);

                this.accountSequenceNumber.put(accountID, record.getSequenceNumber());

                // check if we want to process the account-pillID pair
                if (this.pillData.get(pillID).size() >= this.processThreshold) {
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
        for (String pillID : toProcessPillIds) {
            LOGGER.debug("Processing Pill data for ID {}", pillID);
            SleepScore score = this.computeSleepScore(this.pillAccountID.get(pillID), pillID, this.pillData.get(pillID));
            final boolean saved = this.saveScore(score);
            if (saved) {
                this.numPillsProcessed++;
            }
        }

        if (this.numPillsProcessed % this.MAX_PILLS_PROCESSED == 0) {
            // TODO: checkpoint everytime we have processed some numbers of pills
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

    private SleepScore computeSleepScore(String accountID, String pillID, SortedSet<SensorSample> pillData) {
        final SensorSample firstData = pillData.first();
        final DateTime dateHourUTC = firstData.dateTime;
        final int timeZoneOffset = firstData.timeZoneOffset;

        int agitationNum = 0;
        float agitationTot = 0;
        int duration = 0;
        for (final SensorSample data: pillData) {
            final float value = data.val;
            if (value != -1) {
                agitationNum++;
                agitationTot = agitationTot + value;
            }
            duration++;
        }
        final int score = (int) (((double) agitationNum)/((double) duration) * 100.0);
        SleepScore sleepScore = new SleepScore(0L,
                Long.parseLong(accountID),
                dateHourUTC,
                Long.parseLong(pillID),
                duration,
                score,
                false,
                this.EMPTY_STRING,
                agitationNum,
                (long) agitationTot,
                DateTime.now(),
                firstData.timeZoneOffset
        );

        return sleepScore;
    }

    private boolean saveScore(SleepScore score) {
        long saved = 0;
        Optional<SleepScore> sleepScoreOptional = this.sleepScoreDAO.getByAccountAndDateHour
                (score.accountId, score.dateHourUTC, score.timeZoneOffset);
        if (sleepScoreOptional.isPresent()) {
            saved = this.sleepScoreDAO.updateBySleepScoreId(sleepScoreOptional.get().id, score.totalHourScore);
        } else {
            saved = this.sleepScoreDAO.insert(score.accountId, score.dateHourUTC, score.pillID,
                    score.timeZoneOffset, score.sleepDuration, score.custom,
                    score.totalHourScore, score.saxSymbols,
                    score.agitationNum, score.agitationTot,
                    score.updated);
        }

        if (saved > 0)
            return true;
        return false;
    }
}

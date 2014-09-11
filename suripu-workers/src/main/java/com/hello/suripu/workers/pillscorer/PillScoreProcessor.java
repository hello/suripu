package com.hello.suripu.workers.pillscorer;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.models.SleepScore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PillScoreProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreProcessor.class);

    private final SleepScoreDAO sleepScoreDAO;
    private int processThreshold; // process data every this number of records

    private final Map<String, List<InputProtos.PillDataKinesis>> accountPillData = new HashMap<>();
    private final Map<String, List<String>> accountSequenceNumber = new HashMap<>();


    private static final String EMPTY_STRING = "";

    public PillScoreProcessor(final SleepScoreDAO sleepScoreDAO, final int processThreshold) {
        this.sleepScoreDAO = sleepScoreDAO;
        this.processThreshold = processThreshold;
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        final ArrayList<String> toProcessIds = new ArrayList<String>();

        for(Record record : records) {
            LOGGER.debug("PartitionKey: {}", record.getPartitionKey());

            try {
                final InputProtos.PillDataKinesis data = InputProtos.PillDataKinesis.parseFrom(record.getData().array());
                final String accountID = data.getAccountId();
                LOGGER.debug("Pill data for {}", accountID);
                if (!this.accountPillData.containsKey(accountID)) {
                    this.accountPillData.put(accountID, new ArrayList<InputProtos.PillDataKinesis>());
                    this.accountSequenceNumber.put(accountID, new ArrayList<String>());
                }
                this.accountPillData.get(accountID).add(data);
                this.accountSequenceNumber.get(accountID).add(record.getSequenceNumber());
                if (this.accountSequenceNumber.get(accountID).size() >= 5) { // this.PROCESS_THRESHOLD) {
                    toProcessIds.add(accountID); // TODO need unique list
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
                // TODO: increment error counter somewhere
            }
        }
        // what happens when we don't have 15 mins of data, say pill died
        for (String accountId : toProcessIds) {
            SleepScore score = this.computeSleepScore(accountId, this.accountPillData.get(accountId));
            this.saveScore(score);
        }

    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }

    private SleepScore computeSleepScore(String accountID, List<InputProtos.PillDataKinesis> pillData) {
        final InputProtos.PillDataKinesis firstData = pillData.get(0);
        final Long firstTimestamp = firstData.getTimestamp();
        final DateTime firstDateTime = new DateTime(firstTimestamp, DateTimeZone.UTC);
        final DateTime dateHourUTC = new DateTime(firstDateTime.getYear(),
                firstDateTime.getMonthOfYear(),
                firstDateTime.getDayOfMonth(),
                firstDateTime.getHourOfDay(),
                0,  // zero-th minute
                DateTimeZone.UTC);
        final int timeZoneOffset = firstData.getOffsetMillis();

        int agitationNum = 0;
        long agitationTot = 0;
        int duration = 0;
        for (InputProtos.PillDataKinesis data: pillData) {
            final long value = data.getValue();
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
                Long.parseLong(firstData.getPillId()),
                duration,
                score,
                false,
                this.EMPTY_STRING,
                agitationNum,
                agitationTot,
                DateTime.now(),
                firstData.getOffsetMillis()
        );

        return sleepScore;
    }

    private void saveScore(SleepScore score) {
        Optional<SleepScore> sleepScoreOptional = this.sleepScoreDAO.getByAccountAndDateHour
                (score.accountId, score.dateHourUTC, score.timeZoneOffset);
        if (sleepScoreOptional.isPresent()) {
            this.sleepScoreDAO.updateBySleepScoreId(sleepScoreOptional.get().id, score.totalHourScore);
        } else {
            this.sleepScoreDAO.insert(score.accountId, score.dateHourUTC, score.pillID,
                    score.timeZoneOffset, score.sleepDuration, score.custom,
                    score.totalHourScore, score.saxSymbols,
                    score.agitationNum, score.agitationTot,
                    score.updated);
        }
    }
}

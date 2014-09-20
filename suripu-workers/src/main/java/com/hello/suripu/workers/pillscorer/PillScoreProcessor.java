package com.hello.suripu.workers.pillscorer;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.core.processors.PillProcessor;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.models.SensorSample;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PillScoreProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreProcessor.class);

    private final PillProcessor pillProcessor;
    private int decodeErrors = 0;

    public PillScoreProcessor(final SleepScoreDAO sleepScoreDAO, final int dateMinuteBucket, final int checkpointThreshold) {
        this.pillProcessor = new PillProcessor(sleepScoreDAO, dateMinuteBucket, checkpointThreshold);
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        // parse kinesis records
        final ListMultimap<Long, SensorSample> samples = ArrayListMultimap.create();
        for (final Record record : records) {
            try {
                final InputProtos.PillDataKinesis data = InputProtos.PillDataKinesis.parseFrom(record.getData().array());

                final Long accountID = Long.parseLong(data.getAccountId());
                final String pillID = data.getPillId();

                final DateTime sampleDT = new DateTime(data.getTimestamp(), DateTimeZone.UTC).withSecondOfMinute(0);

                final SensorSample sample = new SensorSample(sampleDT, data.getValue(), data.getOffsetMillis());
                sample.setID(pillID);
                samples.put(accountID, sample);

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
                this.decodeErrors++;
            }
        }
        final boolean okayToCheckpoint = this.pillProcessor.processPillRecords(samples);

        if (okayToCheckpoint) {
            try {
                iRecordProcessorCheckpointer.checkpoint();
            } catch (InvalidStateException e) {
                LOGGER.error("checkpoint {}", e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
            }
        }
    }

    // james was here
    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }

}
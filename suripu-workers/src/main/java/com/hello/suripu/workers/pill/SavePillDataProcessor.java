package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SavePillDataProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(SavePillDataProcessor.class);

    private final TrackerMotionDAO trackerMotionDAO;
    private final int batchSize;

    private int decodeErrors = 0;

    public SavePillDataProcessor(final TrackerMotionDAO trackerMotionDAO, final int batchSize) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.batchSize = batchSize;
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        // parse kinesis records
        ArrayList<TrackerMotion> trackerData = new ArrayList<>();
        for (final Record record : records) {
            try {
                final InputProtos.PillDataKinesis data = InputProtos.PillDataKinesis.parseFrom(record.getData().array());

                final Long accountID = Long.parseLong(data.getAccountId());
                final Long pillID = Long.parseLong(data.getPillId());
                final DateTime sampleDT = new DateTime(data.getTimestamp(), DateTimeZone.UTC);
                final DateTime roundedDateTimeUTC = new DateTime(
                        sampleDT.getYear(),
                        sampleDT.getMonthOfYear(),
                        sampleDT.getDayOfMonth(),
                        sampleDT.getHourOfDay(),
                        sampleDT.getMinuteOfHour(),
                        DateTimeZone.UTC
                );
                final TrackerMotion trackerMotion = new TrackerMotion(
                        0,
                        accountID,
                        pillID,
                        roundedDateTimeUTC.getMillis(),
                        (int) data.getValue(),
                        data.getOffsetMillis()
                );
                trackerData.add(trackerMotion);
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
                this.decodeErrors++;
            }
        }

        this.trackerMotionDAO.batchInsertTrackerMotionData(trackerData, this.batchSize);

        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }

    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }




}

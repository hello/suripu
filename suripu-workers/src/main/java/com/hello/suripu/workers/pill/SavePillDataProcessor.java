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
        final ArrayList<TrackerMotion> trackerData = new ArrayList<>();
        for (final Record record : records) {
            try {
                final InputProtos.PillDataKinesis data = InputProtos.PillDataKinesis.parseFrom(record.getData().array());

                final Long accountID = data.hasAccountIdLong() ? data.getAccountIdLong() : Long.parseLong(data.getAccountId());
                final Long pillID = data.hasPillIdLong() ? data.getPillIdLong() : Long.parseLong(data.getPillId());
                final DateTime sampleDT = new DateTime(data.getTimestamp(), DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
                long amplitudeMilliG = -1;
                if(data.hasValue()){
                    amplitudeMilliG = data.getValue();
                }

                if(data.hasEncryptedData()){
                    final byte[] fakeKey = new byte[16];
                    final byte[] encryptedData = data.getEncryptedData().toByteArray();
                    try {
                        final long raw = TrackerMotion.Utils.encryptedToRaw(fakeKey, encryptedData);
                        amplitudeMilliG = TrackerMotion.Utils.rawToMilliMS2(raw);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Failed to decrypted pill data for pill id {}, error: {}", pillID, e.getMessage());
                    }
                }

                if(amplitudeMilliG != -1) {
                    final TrackerMotion trackerMotion = new TrackerMotion(
                            0L,
                            accountID,
                            pillID,
                            sampleDT.getMillis(),
                            (int) data.getValue(),
                            data.getOffsetMillis()
                    );
                    trackerData.add(trackerMotion);
                }else{
                    //TODO: Deal with heartbeat
                    final int batteryLevel = data.getBatteryLevel();
                    final int upTime = data.getUpTime();
                    final int firmwareVersion = data.getFirmwareVersion();

                    // TODO: Save the heartbeat
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            }
        }

        if (trackerData.size() > 0) {
            this.trackerMotionDAO.batchInsertTrackerMotionData(trackerData, this.batchSize);

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




}

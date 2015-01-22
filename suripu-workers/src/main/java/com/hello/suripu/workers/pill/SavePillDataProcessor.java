package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SavePillDataProcessor extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(SavePillDataProcessor.class);

    private final TrackerMotionDAO trackerMotionDAO;
    private final int batchSize;
    private final PillHeartBeatDAO pillHeartBeatDAO;
    private final KeyStore pillKeyStore;

    public SavePillDataProcessor(final TrackerMotionDAO trackerMotionDAO, final int batchSize, final PillHeartBeatDAO pillHeartBeatDAO, final KeyStore pillKeyStore) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.batchSize = batchSize;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.pillKeyStore = pillKeyStore;
    }

    @Override
    public void initialize(String s) {
    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        // parse kinesis records
        final ArrayList<TrackerMotion> trackerData = new ArrayList<>(records.size());
        for (final Record record : records) {
            try {
                final InputProtos.PillDataKinesis data = InputProtos.PillDataKinesis.parseFrom(record.getData().array());
                final Optional<byte[]> decryptionKey = pillKeyStore.get(data.getPillId());
                //TODO: Get the actual decryption key.
                if(!decryptionKey.isPresent()) {
                    LOGGER.error("Missing decryption key for pill: {}", data.getPillId());
                    continue;
                }
                TrackerMotion trackerMotion = null;
                if(data.hasEncryptedData()){
                    switch (data.getFirmwareVersion()){
                        case 0:
                        case 1:
                            trackerMotion = new TrackerMotion.Builder().withPillKinesisData(decryptionKey.get(), data).build();
                            LOGGER.info("Short data received from pill {}", data.getPillId());
                            break;
                        case 2:
                            // TODO: add column to tracker_motion table to save acceleration range and kickoff time
                            // TODO: modify TrackerMotion & its mapper to have those two attributes
                            trackerMotion = new TrackerMotion.Builder().withPillKinesisDataVersion2(decryptionKey.get(), data).build();
                            LOGGER.info("Long data received from pill {}", data.getPillId());
                            break;
                        default:
                            LOGGER.warn("Unknown firmware version of pill data from pill {}", data.getPillId());
                    }

                    if(trackerMotion != null){
                        trackerData.add(trackerMotion);
                    }
                }

                if(data.hasBatteryLevel()){
                    final int batteryLevel = data.getBatteryLevel();
                    final int upTime = data.getUpTime();
                    final int firmwareVersion = data.getFirmwareVersion();
                    final DateTime lastUpdated = new DateTime(data.getTimestamp(), DateTimeZone.UTC);
                    pillHeartBeatDAO.silentInsert(data.getPillIdLong(), batteryLevel, upTime, firmwareVersion, lastUpdated);
                }
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to decrypted pill data {}, error: {}", record.getData().array(), e.getMessage());
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

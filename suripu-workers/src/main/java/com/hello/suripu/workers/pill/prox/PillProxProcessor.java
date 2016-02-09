package com.hello.suripu.workers.pill.prox;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.PillProxDataDAODynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.PillProxData;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeat;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PillProxProcessor extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillProxProcessor.class);


    private final PillProxDataDAODynamoDB pillProxDataDAODynamoDB;
    private final int batchSize;
    private final KeyStore pillKeyStore;
    private final DeviceDAO deviceDAO;

    private final Meter messagesProcessed;
    private final Meter batchSaved;

    public PillProxProcessor(final PillProxDataDAODynamoDB pillProxDataDAODynamoDB,
                             final int batchSize,
                             final KeyStore pillKeyStore,
                             final DeviceDAO deviceDAO) {

        this.batchSize = batchSize;
        this.pillKeyStore = pillKeyStore;
        this.pillProxDataDAODynamoDB = pillProxDataDAODynamoDB;
        this.deviceDAO = deviceDAO;

        this.messagesProcessed = Metrics.defaultRegistry().newMeter(PillProxProcessor.class, "messages", "messages-processed", TimeUnit.SECONDS);
        this.batchSaved = Metrics.defaultRegistry().newMeter(PillProxProcessor.class, "batch", "batch-saved", TimeUnit.SECONDS);
    }

    @Override
    public void initialize(String s) {
    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        // parse kinesis records
        final ArrayList<TrackerMotion> trackerData = new ArrayList<>(records.size());
        final Set<PillHeartBeat> pillHeartBeats = Sets.newHashSet(); // for dynamoDB writes

        final Map<String, Optional<byte[]>> pillKeys = Maps.newHashMap();
        final Map<String, Optional<DeviceAccountPair>> pairs = Maps.newHashMap();

        final Set<String> pillIds = Sets.newHashSet();


        final List<SenseCommandProtos.pill_data> proxData = Lists.newArrayList();
        final List<PillProxData> pillProxDataList = Lists.newArrayList();

        for (final Record record : records) {
            try {
                final SenseCommandProtos.batched_pill_data batched_pill_data = SenseCommandProtos.batched_pill_data.parseFrom(record.getData().array());
                for(final SenseCommandProtos.pill_data data: batched_pill_data.getProxList()) {
                    pillIds.add(data.getDeviceId());
                    proxData.add(data);
                }

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to decrypted pill data {}, error: {}", record.getData().array(), e.getMessage());
            }
        }

        try {
            // Fetch data from Dynamo and DB
            if(pillIds.isEmpty()) {
//                LOGGER.warn("No valid prox pills");
                return;
            }

            final Map<String, Optional<byte[]>> keys = pillKeyStore.getBatch(pillIds);
            if(keys.isEmpty()) {
                LOGGER.error("Failed to retrieve decryption keys. Can't proceed. Bailing");
                System.exit(1);
            }

            pillKeys.putAll(keys);



                // The key should not be null

                for (final SenseCommandProtos.pill_data proxPacket : proxData) {
                    final Optional<byte[]> decryptionKey = pillKeys.get(proxPacket.getDeviceId());
                    if (!decryptionKey.isPresent()) {
                        LOGGER.error("Missing decryption key for pill: {}", proxPacket.getDeviceId());
                        continue;
                    }

                    if (proxPacket.hasMotionDataEntrypted()) {
//                        final Optional<DeviceAccountPair> pair = pairs.get(proxPacket.getDeviceId());
                        final Optional<byte[]> key = keys.get(proxPacket.getDeviceId());
                        final Long accountId = 999L;
                        final long ts = proxPacket.getTimestamp() * 1000L;
                        if (key.isPresent()) {
                            final PillProxData pillProxData = PillProxData.fromEncryptedData(
                                    proxPacket.getDeviceId(),
                                    proxPacket.getMotionDataEntrypted().toByteArray(),
                                    key.get(),
                                    new DateTime(ts, DateTimeZone.UTC)
                            );

                            pillProxDataList.add(pillProxData);
                        }
                    }
            }
        } catch (Exception e) {
            LOGGER.error("Failed processing pill: {}", e.getMessage());
            LOGGER.error("Failed processing pill: {}", e);
        }




        if(!pillProxDataList.isEmpty()) {
            final int proxInsertedCount = pillProxDataDAODynamoDB.batchInsert(pillProxDataList);
            LOGGER.info("action=insert-prox-data count={}", proxInsertedCount);
        }

        if(!trackerData.isEmpty() || !pillHeartBeats.isEmpty()) {
            try {
                iRecordProcessorCheckpointer.checkpoint();
                LOGGER.info("Successful checkpoint.");
            } catch (InvalidStateException e) {
                LOGGER.error("checkpoint {}", e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
            }
        }

        messagesProcessed.mark(records.size());
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
        if(shutdownReason == ShutdownReason.TERMINATE) {
            LOGGER.warn("Got Terminate. Attempting to checkpoint.");
            try {
                iRecordProcessorCheckpointer.checkpoint();
                LOGGER.warn("Checkpoint successful.");
            } catch (InvalidStateException | ShutdownException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }
}

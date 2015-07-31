package com.hello.suripu.workers.insights;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 1/6/15.
 */
public class InsightsGenerator extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(InsightsGenerator.class);

    private final InsightProcessor insightProcessor;
    private final AccountDAO accountDAO;
    private final DeviceDAO deviceDAO;
    private final Map<Long, DateTime> accountCreatedMap;

    public InsightsGenerator(final AccountDAO accountDAO,
                             final DeviceDAO deviceDAO,
                             final InsightProcessor insightProcessor) {
        this.accountDAO = accountDAO;
        this.deviceDAO = deviceDAO;
        this.insightProcessor = insightProcessor;
        accountCreatedMap = new HashMap<>(); // cache account creation date
    }

    @Override
    public void initialize(String s) {

    }

    @Timed
    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("record size {}", records.size());
        for (final Record record : records) {
            try {
                final SenseCommandProtos.batched_pill_data data = SenseCommandProtos.batched_pill_data.parseFrom(record.getData().array());

                for(final SenseCommandProtos.pill_data pill : data.getPillsList()) {
                    if (!pill.hasBatteryLevel()) {
                        continue;
                    }

                    final Optional<DeviceAccountPair> internalPillPairingMap = this.deviceDAO.getInternalPillId(pill.getDeviceId());
                    if (!internalPillPairingMap.isPresent()) {
                        continue;
                    }

                    final Long accountId = internalPillPairingMap.get().accountId;

                    Optional<DateTime> accountCreated = Optional.absent();
                    if (this.accountCreatedMap.containsKey(accountId)) {
                        accountCreated = Optional.of(this.accountCreatedMap.get(accountId));
                    } else {
                        final Optional<Account> accountOptional = this.accountDAO.getById(accountId);
                        if (accountOptional.isPresent()) {
                            accountCreated = Optional.of(accountOptional.get().created);
                            this.accountCreatedMap.put(accountId, accountOptional.get().created);
                        }
                    }

                    if (accountCreated.isPresent()) {
                        LOGGER.debug("Generating Insight for account: {}", accountId);
                        this.insightProcessor.generateInsights(accountId, accountCreated.get(), flipper);
                    }

                }

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to decrypted pill data {}, error: {}", record.getData().array(), e.getMessage());
            }
        }

        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
        if(shutdownReason == ShutdownReason.TERMINATE) {
            LOGGER.warn("Got Termintate. Attempting to checkpoint.");
            try {
                iRecordProcessorCheckpointer.checkpoint();
                LOGGER.warn("Checkpoint successful.");
            } catch (InvalidStateException e) {
                LOGGER.error(e.getMessage());
            } catch (ShutdownException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

}

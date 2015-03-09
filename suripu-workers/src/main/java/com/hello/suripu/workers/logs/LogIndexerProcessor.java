package com.hello.suripu.workers.logs;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LogIndexerProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogIndexerProcessor.class);

    private final LogIndexer<LoggingProtos.BatchLogMessage> applicationIndexer;
    private final LogIndexer<LoggingProtos.BatchLogMessage> senseIndexer;

    private LogIndexerProcessor(final LogIndexer<LoggingProtos.BatchLogMessage> applicationIndexer,
                                final LogIndexer<LoggingProtos.BatchLogMessage> senseIndexer) {
        this.applicationIndexer = applicationIndexer;
        this.senseIndexer = senseIndexer;
    }

    public static LogIndexerProcessor create(final IndexTankClient.Index applicationIndex, final IndexTankClient.Index senseIndex) {
        return new LogIndexerProcessor(new ApplicationLogIndexer(applicationIndex), new SenseLogIndexer(senseIndex));
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        for(final Record record : records) {
            try {
                final LoggingProtos.BatchLogMessage batchLogMessage = LoggingProtos.BatchLogMessage.parseFrom(record.getData().array());

                if(batchLogMessage.hasLogType()) {
                    switch (batchLogMessage.getLogType()) {
                        case APPLICATION_LOG:
                            applicationIndexer.collect(batchLogMessage);
                            break;
                        case SENSE_LOG:
                            senseIndexer.collect(batchLogMessage);
                    }
                } else { // old protobuf messages don't have a LogType
                    applicationIndexer.collect(batchLogMessage);
                }

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed converting protobuf: {}", e.getMessage());
            }
        }

        try {

            final Integer applicationLogsCount = applicationIndexer.index();
            final Integer senseLogsCount = senseIndexer.index();

            iRecordProcessorCheckpointer.checkpoint();
            LOGGER.info("Checkpointing {} records ({} app logs and {} sense logs)", records.size(), applicationLogsCount, senseLogsCount);
        } catch (ShutdownException e) {
            LOGGER.error("Shutdown: {}", e.getMessage());
        } catch (InvalidStateException e) {
            LOGGER.error("Invalid state: {}", e.getMessage());
        }
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.info("Shutting down because: {}", shutdownReason);
    }
}

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
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.db.SenseEventsDAO;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LogIndexerProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogIndexerProcessor.class);

    private final LogIndexer<LoggingProtos.BatchLogMessage> applicationIndexer;
    private final LogIndexer<LoggingProtos.BatchLogMessage> senseIndexer;
    private final LogIndexer<LoggingProtos.BatchLogMessage> workersIndexer;
    private final LogIndexer<LoggingProtos.BatchLogMessage> senseStructuredLogsIndexer;
    private final LogIndexer<LoggingProtos.BatchLogMessage> onBoardingLogIndexer;

    private final Meter applicationLogs;
    private final Meter senseLogs;
    private final Meter workersLogs;
    private final Meter structuredLogs;

    private LogIndexerProcessor(final LogIndexer<LoggingProtos.BatchLogMessage> applicationIndexer,
                                final LogIndexer<LoggingProtos.BatchLogMessage> senseIndexer,
                                final LogIndexer<LoggingProtos.BatchLogMessage> workersIndexer,
                                final LogIndexer<LoggingProtos.BatchLogMessage> senseStructuredLogsIndexer,
                                final LogIndexer<LoggingProtos.BatchLogMessage> onBoardingLogIndexer) {
        this.applicationIndexer = applicationIndexer;
        this.senseIndexer = senseIndexer;
        this.workersIndexer = workersIndexer;
        this.senseStructuredLogsIndexer = senseStructuredLogsIndexer;
        this.onBoardingLogIndexer = onBoardingLogIndexer;

        this.applicationLogs = Metrics.defaultRegistry().newMeter(LogIndexerProcessor.class, "application-logs", "application-processed", TimeUnit.SECONDS);
        this.senseLogs= Metrics.defaultRegistry().newMeter(LogIndexerProcessor.class, "sense-logs", "sense-processed", TimeUnit.SECONDS);
        this.workersLogs = Metrics.defaultRegistry().newMeter(LogIndexerProcessor.class, "workers-logs", "workers-processed", TimeUnit.SECONDS);
        this.structuredLogs = Metrics.defaultRegistry().newMeter(LogIndexerProcessor.class, "structured-logs", "structured-processed", TimeUnit.SECONDS);
    }

    public static LogIndexerProcessor create(final IndexTankClient.Index applicationIndex,
                                             final IndexTankClient.Index senseIndex,
                                             final IndexTankClient.Index workersIndex,
                                             final SenseEventsDAO senseEventsDAO,
                                             final OnBoardingLogDAO onBoardingLogDAO) {
        return new LogIndexerProcessor(
                new GenericLogIndexer(applicationIndex),
                new SenseLogIndexer(senseIndex),
                new GenericLogIndexer(workersIndex),
                new SenseStructuredLogIndexer(senseEventsDAO),
                new OnBoardingLogIndexer(onBoardingLogDAO)
        );
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
                            break;
                        case WORKERS_LOG:
                            workersIndexer.collect(batchLogMessage);
                            break;
                        case STRUCTURED_SENSE_LOG:
                            senseStructuredLogsIndexer.collect(batchLogMessage);
                            break;
                        case ONBOARDING_LOG:
                            this.onBoardingLogIndexer.collect(batchLogMessage);
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
            final Integer workersLogsCount = workersIndexer.index();
            final Integer eventsCount = senseStructuredLogsIndexer.index();
            final Integer onBoardingLogCount = this.onBoardingLogIndexer.index();

            applicationLogs.mark(applicationLogsCount);
            senseLogs.mark(senseLogsCount);
            workersLogs.mark(workersLogsCount);
            structuredLogs.mark(eventsCount);

            iRecordProcessorCheckpointer.checkpoint();
            LOGGER.info("Checkpointing {} records ({} app logs, {} sense logs, {} workers logs, {} kv logs and {} onboarding logs.)",
                    records.size(),
                    applicationLogsCount,
                    senseLogsCount,
                    workersLogsCount,
                    eventsCount,
                    onBoardingLogCount);
        } catch (ShutdownException e) {
            LOGGER.error("Shutdown: {}", e.getMessage());
        } catch (InvalidStateException e) {
            LOGGER.error("Invalid state: {}", e.getMessage());
        }
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.warn("Shutting down because: {}", shutdownReason);
        System.exit(1);
    }
}

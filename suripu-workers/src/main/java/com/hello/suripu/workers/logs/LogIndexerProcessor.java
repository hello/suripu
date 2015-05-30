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
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LogIndexerProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogIndexerProcessor.class);

    private final LogIndexer<LoggingProtos.BatchLogMessage> senseIndexer;
    private final LogIndexer<LoggingProtos.BatchLogMessage> senseStructuredLogsIndexer;
    private final LogIndexer<LoggingProtos.BatchLogMessage> onBoardingLogIndexer;

    private final Meter senseLogs;
    private final Meter structuredLogs;
    private final Meter onboardingLogs;

    private LogIndexerProcessor(final LogIndexer<LoggingProtos.BatchLogMessage> senseIndexer,
                                final LogIndexer<LoggingProtos.BatchLogMessage> senseStructuredLogsIndexer,
                                final LogIndexer<LoggingProtos.BatchLogMessage> onBoardingLogIndexer) {
        this.senseIndexer = senseIndexer;
        this.senseStructuredLogsIndexer = senseStructuredLogsIndexer;
        this.onBoardingLogIndexer = onBoardingLogIndexer;

        this.senseLogs= Metrics.defaultRegistry().newMeter(LogIndexerProcessor.class, "sense-logs", "sense-processed", TimeUnit.SECONDS);
        this.structuredLogs = Metrics.defaultRegistry().newMeter(LogIndexerProcessor.class, "structured-logs", "structured-processed", TimeUnit.SECONDS);
        this.onboardingLogs  = Metrics.defaultRegistry().newMeter(LogIndexerProcessor.class, "onboarding-logs", "onboarding-processed", TimeUnit.SECONDS);
    }

    public static LogIndexerProcessor create(final IndexTankClient indexTankClient,
                                             final String senseLogIndexPrefix,
                                             final IndexTankClient.Index senseLogBackupIndex,
                                             final SenseEventsDAO senseEventsDAO,
                                             final OnBoardingLogDAO onBoardingLogDAO,
                                             final JedisPool jedisPool) {
        return new LogIndexerProcessor(
                new SenseLogIndexer(indexTankClient, senseLogIndexPrefix, senseLogBackupIndex, jedisPool),
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

                        case SENSE_LOG:
                            senseIndexer.collect(batchLogMessage);
                            break;

                        case STRUCTURED_SENSE_LOG:
                            senseStructuredLogsIndexer.collect(batchLogMessage);
                            break;
                        case ONBOARDING_LOG:
                            this.onBoardingLogIndexer.collect(batchLogMessage);
                    }
                }

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed converting protobuf: {}", e.getMessage());
            }
        }

        try {
            final Integer senseLogsCount = senseIndexer.index();
            final Integer eventsCount = senseStructuredLogsIndexer.index();
            final Integer onBoardingLogCount = this.onBoardingLogIndexer.index();

            senseLogs.mark(senseLogsCount);
            structuredLogs.mark(eventsCount);
            onboardingLogs.mark(onBoardingLogCount);

            iRecordProcessorCheckpointer.checkpoint();
            LOGGER.info("Checkpointing {} records ({} sense logs, {} kv logs and {} onboarding logs.)",
                    records.size(),
                    senseLogsCount,
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

package com.hello.suripu.workers.logs;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.models.ElasticSearch.ElasticSearchBulkSettings;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.elasticsearch.client.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ElasticSearchIndexerProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogIndexerProcessor.class);

    private final LogIndexer<LoggingProtos.BatchLogMessage> senseLogElasticSearchIndexer;

    private final Meter senseLogs;

    private ElasticSearchIndexerProcessor(final LogIndexer<LoggingProtos.BatchLogMessage> senseLogElasticSearchIndexer){
        this.senseLogElasticSearchIndexer = senseLogElasticSearchIndexer;
        this.senseLogs= Metrics.defaultRegistry().newMeter(ElasticSearchIndexerProcessor.class, "es-sense-logs", "es-sense-processed", TimeUnit.SECONDS);
    }

    public static ElasticSearchIndexerProcessor create(final JedisPool jedisPool, final TransportClient transportClient, final ElasticSearchBulkSettings elasticSearchBulkSettings, final String indexPrefix) {

        return new ElasticSearchIndexerProcessor(new ElasticSearchLogIndexer(jedisPool, transportClient, elasticSearchBulkSettings, indexPrefix));
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
                            senseLogElasticSearchIndexer.collect(batchLogMessage);
                            break;

                        default:
                            LOGGER.debug("Not going to index because log type is {}", batchLogMessage.getLogType());

                    }
                }

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed converting protobuf: {}", e.getMessage());
            }
        }

        try {
            final Integer senseLogsElasticSearchCount = senseLogElasticSearchIndexer.index();
            senseLogs.mark(senseLogsElasticSearchCount);

            iRecordProcessorCheckpointer.checkpoint();
            LOGGER.info("Checkpointing {} sense logs out of {} records", senseLogsElasticSearchCount, records.size());

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

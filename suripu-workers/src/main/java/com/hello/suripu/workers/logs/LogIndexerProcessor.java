package com.hello.suripu.workers.logs;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.flaptor.indextank.apiclient.Index;
import com.flaptor.indextank.apiclient.IndexDoesNotExistException;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.workers.pill.LogChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogIndexerProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogIndexerProcessor.class);
    private final Index index;

    public LogIndexerProcessor(final IndexTankClient.Index index) {
        this.index = index;
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final List<IndexTankClient.Document> documents = new ArrayList<>();
        for(final Record record : records) {
            try {
                final LoggingProtos.BatchLogMessage batchLogMessage = LoggingProtos.BatchLogMessage.parseFrom(record.getData().array());
                documents.addAll(LogChunker.chunkBatchLogMessage(batchLogMessage));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed converting protobuf: {}", e.getMessage());
            }
        }

        try {

            if(!documents.isEmpty()) {
                index.addDocuments(documents);
                LOGGER.info("Indexed {} documents", documents.size());
            }

            iRecordProcessorCheckpointer.checkpoint();
            LOGGER.info("Checkpointing {} records ({} documents)", records.size(), documents.size());
        } catch (ShutdownException e) {
            e.printStackTrace();
        } catch (IndexDoesNotExistException e) {
            LOGGER.error("Index does not exist: {}", e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            LOGGER.error("Failed connecting to searchify: {}", e.getMessage());
        } catch (InvalidStateException e) {
            LOGGER.error("Invalid state: {}", e.getMessage());
        }
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.info("Shutting down because: {}", shutdownReason);
    }
}

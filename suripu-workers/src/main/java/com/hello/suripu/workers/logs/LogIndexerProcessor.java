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
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogIndexerProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogIndexerProcessor.class);
    private final Index index;

    public LogIndexerProcessor(final IndexTankClient.Index index) {
        this.index = index;
    }

    @Override
    public void initialize(String s) {

    }

    private IndexTankClient.Document pbToDocument(final LoggingProtos.LogMessage currentMessage, LoggingProtos.LogMessage startLogMessage, final String messageString) {

        final Map<String, String> documentAttributes = new HashMap<>();
        final Map<Integer, Float> variables = new HashMap<>();
        final Map<String, String> categories = new HashMap<>();

        documentAttributes.put("text", messageString);
        documentAttributes.put("ts", String.valueOf(startLogMessage.getTs()));

        categories.put("origin", startLogMessage.getOrigin());
        categories.put("level", Level.toLevel(startLogMessage.getLevel()).toString());

        variables.put(0, new Float(startLogMessage.getTs()));

        final String id = String.format("%s-%s", startLogMessage.getOrigin(), startLogMessage.getTs());
        return new IndexTankClient.Document(id, documentAttributes, variables, categories);
    }

    private List<IndexTankClient.Document> batchProtobufToDocuments(final LoggingProtos.BatchLogMessage batchLogMessage) {
        final List<IndexTankClient.Document> documents = new ArrayList<>();
        if(batchLogMessage.getMessagesList().isEmpty()) {
            LOGGER.warn("Batch was empty, no conversion needed");
            return documents;
        }

        final StringBuffer sb = new StringBuffer();
        Integer startIndex = 0;
        Integer startLevel = batchLogMessage.getMessages(startIndex).getLevel();

        for(int i = 0; i < batchLogMessage.getMessagesCount(); i ++) {

            final LoggingProtos.LogMessage current = batchLogMessage.getMessages(i);
            if(current.hasLevel() && current.getLevel() == startLevel) {
                sb.append(current.getMessage() + "\n");
            } else {
                final IndexTankClient.Document doc = pbToDocument(current, batchLogMessage.getMessages(startIndex), sb.toString());
                documents.add(doc);
                startIndex = i;
                startLevel = current.getLevel();
                sb.setLength(0); // reset it
            }
            i++;
        }

        return documents;
    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        final List<IndexTankClient.Document> documents = new ArrayList<>();
        for(final Record record : records) {
            try {
                final LoggingProtos.BatchLogMessage batchLogMessage = LoggingProtos.BatchLogMessage.parseFrom(record.getData().array());
                documents.addAll(batchProtobufToDocuments(batchLogMessage));
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed converting protobuf: {}", e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {

            if(!documents.isEmpty()) {
                index.addDocuments(documents);
                LOGGER.info("Indexed {} documents", documents.size());
            }

            iRecordProcessorCheckpointer.checkpoint();

        } catch (ShutdownException e) {
            e.printStackTrace();
        } catch (InvalidStateException e) {
            e.printStackTrace();
        } catch (IndexDoesNotExistException e) {
            LOGGER.error("Index does not exist: {}", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {

    }
}

package com.hello.suripu.workers.logs;

import com.flaptor.indextank.apiclient.IndexDoesNotExistException;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.logging.LoggingProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SenseLogIndexer implements LogIndexer<LoggingProtos.BatchLogMessage> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLogIndexer.class);

    private final IndexTankClient.Index index;
    final private List<IndexTankClient.Document> documents;

    public SenseLogIndexer(final IndexTankClient.Index index) {
        this.index = index;
        documents = Lists.newArrayList();
    }


    public static List<IndexTankClient.Document> chunkBatchLogMessage(LoggingProtos.BatchLogMessage batchLogMessage) {
        final List<IndexTankClient.Document> documents = Lists.newArrayList();
        for(final LoggingProtos.LogMessage log : batchLogMessage.getMessagesList()) {
            final Long millis = (log.getTs() == 0) ? batchLogMessage.getReceivedAt() : log.getTs() * 1000L;
            final String documentId = String.format("%s-%d", log.getDeviceId(), millis);

            final Map<String, String> fields = Maps.newHashMap();
            final Map<String, String> categories = Maps.newHashMap();

            fields.put("device_id", log.getDeviceId());
            fields.put("text", log.getMessage());
            fields.put("ts", String.valueOf(log.getTs()));

            final Long hello_ts = millis;


            final Map<Integer, Float> variables = new HashMap<>();
            variables.put(0, new Float(hello_ts / 1000));

            categories.put("device_id", log.getDeviceId());

            documents.add(new IndexTankClient.Document(documentId, fields, variables, categories));

        }
        return documents;
    }

    @Override
    public Integer index() {
        try {
            if (!documents.isEmpty()) {
                index.addDocuments(ImmutableList.copyOf(documents));
                final Integer count = documents.size();
                LOGGER.info("Indexed {} documents", count);
                documents.clear();
                return count;
            }
        } catch (IndexDoesNotExistException e) {
            LOGGER.error("Index does not exist: {}", e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            LOGGER.error("Failed connecting to searchify: {}", e.getMessage());
        } catch(IndexOutOfBoundsException e) {
            LOGGER.error("Searchify client error: {}", e.getMessage());
        }

        return 0;
    }

    @Override
    public void collect(final LoggingProtos.BatchLogMessage batchLogMessage) {
        documents.addAll(chunkBatchLogMessage(batchLogMessage));
    }
}

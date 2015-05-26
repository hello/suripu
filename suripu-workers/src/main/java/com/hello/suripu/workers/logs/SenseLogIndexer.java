package com.hello.suripu.workers.logs;

import com.flaptor.indextank.apiclient.IndexDoesNotExistException;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.logging.LoggingProtos;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SenseLogIndexer implements LogIndexer<LoggingProtos.BatchLogMessage> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLogIndexer.class);

    private final IndexTankClient indexTankClient;
    private final String senseLogIndexPrefix;
    private final IndexTankClient.Index senseLogBackupIndex;
    private final List<IndexTankClient.Document> documents;
    private final Map<String, IndexTankClient.Index> indexes;
    private IndexTankClient.Index index;

    public SenseLogIndexer(final IndexTankClient indexTankClient, final String senseLogIndexPrefix, final IndexTankClient.Index senseLogBackupIndex) {
        this.indexTankClient = indexTankClient;
        this.senseLogIndexPrefix = senseLogIndexPrefix;
        this.senseLogBackupIndex = senseLogBackupIndex;
        this.documents = Lists.newArrayList();
        this.indexes = Maps.newHashMap();
        this.index = senseLogBackupIndex;
    }


    public static BatchLog chunkBatchLogMessage(LoggingProtos.BatchLogMessage batchLogMessage) {
        final List<IndexTankClient.Document> documents = Lists.newArrayList();
        String createdDateString = new DateTime(DateTimeZone.UTC).toString(DateTimeFormat.forPattern("yyyy-MM-dd"));
        for(final LoggingProtos.LogMessage log : batchLogMessage.getMessagesList()) {
            final Long millis = (log.getTs() == 0) ? batchLogMessage.getReceivedAt() : log.getTs() * 1000L;
            final String documentId = String.format("%s-%d", log.getDeviceId(), millis);
            final DateTime createdDateTime = new DateTime(millis, DateTimeZone.UTC);
            final String halfDateString = createdDateTime.toString(DateTimeFormat.forPattern("yyyyMMdda"));
            final String dateString = createdDateTime.toString(DateTimeFormat.forPattern("yyyyMMdd"));

            final Map<String, String> fields = Maps.newHashMap();
            final Map<String, String> categories = Maps.newHashMap();

            fields.put("device_id", log.getDeviceId());
            fields.put("text", log.getMessage());
            fields.put("ts", String.valueOf(log.getTs()));
            fields.put("half_date", halfDateString);
            fields.put("date", dateString);
            fields.put("all", "1");


            createdDateString = createdDateTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd"));

            final Map<Integer, Float> variables = new HashMap<>();
            variables.put(0, new Float(millis / 1000));
            variables.put(1, new Float(millis));

            categories.put("device_id", log.getDeviceId());
            categories.put("origin", log.getOrigin());
            categories.put("half_date", halfDateString);
            categories.put("date", dateString);

            documents.add(new IndexTankClient.Document(documentId, fields, variables, categories));

        }
        return new BatchLog(documents, createdDateString);
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

        final BatchLog batchLog = chunkBatchLogMessage(batchLogMessage);
        documents.addAll(batchLog.documents);

        if (!indexes.containsKey(batchLog.createdDateString)){
            IndexTankClient.Index newIndex;
            try {
                newIndex = indexTankClient.createIndex(senseLogIndexPrefix + batchLog.createdDateString);
            }
            catch (Exception e) {
                newIndex = senseLogBackupIndex;
                LOGGER.error("Failed to create new index because {} ", e.getMessage());
            }
            indexes.put(batchLog.createdDateString, newIndex);
        }
        index = indexes.get(batchLog.createdDateString);
    }

    private static class BatchLog {
        public final List<IndexTankClient.Document> documents;
        public final String createdDateString;
        public BatchLog(final List<IndexTankClient.Document> documents, final String createdDateString) {
            this.documents = documents;
            this.createdDateString = createdDateString;
        }
    }

}

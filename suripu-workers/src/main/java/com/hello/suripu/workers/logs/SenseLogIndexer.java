package com.hello.suripu.workers.logs;

import com.flaptor.indextank.apiclient.IndexDoesNotExistException;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SenseLogIndexer implements LogIndexer<LoggingProtos.BatchLogMessage> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseLogIndexer.class);

    private final IndexTankClient.Index index;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    final private List<IndexTankClient.Document> documents;

    public SenseLogIndexer(final IndexTankClient.Index index, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
        this.index = index;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        documents = Lists.newArrayList();
    }


    public List<IndexTankClient.Document> chunkBatchLogMessage(LoggingProtos.BatchLogMessage batchLogMessage) {
        final List<IndexTankClient.Document> documents = Lists.newArrayList();
        for(final LoggingProtos.LogMessage log : batchLogMessage.getMessagesList()) {
            final Long millis = (log.getTs() == 0) ? batchLogMessage.getReceivedAt() : log.getTs() * 1000L;
            final String documentId = String.format("%s-%d", log.getDeviceId(), millis);

            final Map<String, String> fields = Maps.newHashMap();
            final Map<String, String> categories = Maps.newHashMap();
            final Integer timezoneOffsetMillis = getTimezoneBySenseIdOnEvent(log.getDeviceId(), new DateTime(log.getTs() * 1000));
            fields.put("device_id", log.getDeviceId());
            fields.put("text", log.getMessage());
            fields.put("ts", String.valueOf(log.getTs()));
            fields.put("all", "1");
            fields.put("timezone", String.valueOf(timezoneOffsetMillis));


            final Long hello_ts = millis;


            final Map<Integer, Float> variables = new HashMap<>();
            variables.put(0, new Float(hello_ts / 1000));

            categories.put("device_id", log.getDeviceId());
            categories.put("origin", log.getOrigin());

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

    private Integer getTimezoneBySenseIdOnEvent(final String senseId, final DateTime eventDateTime) {
        final List<UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(senseId);
        if (userInfoList.isEmpty()) {
            return -25200000;  // PST;
        }

        final Optional<DateTimeZone> timezoneOptional = mergedUserInfoDynamoDB.getTimezone(senseId, userInfoList.get(0).accountId);
        if (!timezoneOptional.isPresent()) {
            return -25200000;  // PST;
        }

        return timezoneOptional.get().getOffset(eventDateTime);
    }
}

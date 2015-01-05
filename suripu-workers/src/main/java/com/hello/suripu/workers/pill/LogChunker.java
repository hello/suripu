package com.hello.suripu.workers.pill;

import com.flaptor.indextank.apiclient.IndexTankClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.api.logging.LoggingProtos;
import org.apache.log4j.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogChunker {


    /**
     * Creates on Search Document from multiple log messages
     * Metadata is inferred from the first document
     * @param messages
     * @return
     */
    public static IndexTankClient.Document merge(final List<LoggingProtos.LogMessage> messages) {
        if(messages.isEmpty()) {
            throw new RuntimeException("List of messages to merge can not be empty");
        }

        final StringBuilder sb = new StringBuilder();
        for(final LoggingProtos.LogMessage message : messages) {
            sb.append(message.getMessage());
            sb.append("\n");
        }

        final Map<String, String> documentAttributes = new HashMap<>();
        final Map<Integer, Float> variables = new HashMap<>();
        final Map<String, String> categories = new HashMap<>();

        documentAttributes.put("text", sb.toString());
        documentAttributes.put("ts", String.valueOf(messages.get(0).getTs()));

        categories.put("origin", messages.get(0).getOrigin());
        categories.put("level", Level.toLevel(messages.get(0).getLevel()).toString());

        variables.put(0, new Float(messages.get(0).getTs()));

        final String id = String.format("%s-%s", messages.get(0).getOrigin(), messages.get(0).getTs());
        return new IndexTankClient.Document(id, documentAttributes, variables, categories);
    }

    /**
     * Group by LogLevel.
     * @param batchLogMessage
     * @return
     */
    public static List<IndexTankClient.Document> chunkBatchLogMessage(LoggingProtos.BatchLogMessage batchLogMessage) {
        final List<IndexTankClient.Document> documents = new ArrayList<>();
        if(batchLogMessage.getMessagesList().isEmpty()) {
            return documents;
        }

        final List<LoggingProtos.LogMessage> buffer = Lists.newArrayList(batchLogMessage.getMessages(0));
        Integer previousLevel = batchLogMessage.getMessages(0).getLevel();
        for(final LoggingProtos.LogMessage message : batchLogMessage.getMessagesList()) {
            if(message.hasLevel() && message.getLevel() != previousLevel) {
                documents.add(merge(ImmutableList.copyOf(buffer)));
                buffer.clear();
                previousLevel = message.getLevel();
            }
            buffer.add(message);
        }

        // Flush buffer might contain consecutive messages with same log level
        if(!buffer.isEmpty()) {
            documents.add(merge(ImmutableList.copyOf(buffer)));
        }
        return documents;
    }
}

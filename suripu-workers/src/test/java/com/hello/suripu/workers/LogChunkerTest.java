package com.hello.suripu.workers;

import ch.qos.logback.classic.Level;
import com.flaptor.indextank.apiclient.IndexTankClient;
import com.google.common.collect.Lists;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.workers.pill.LogChunker;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogChunkerTest {

    @Test
    public void testConvertProtobufToDocument() {
        final Long ts = DateTime.now().getMillis();
        final LoggingProtos.LogMessage message = LoggingProtos.LogMessage.newBuilder()
                .setMessage("ONE TWO THREE")
                .setLevel(Level.DEBUG.toInteger())
                .setOrigin("test")
                .setProduction(false)
                .setTs(ts).build();

        final IndexTankClient.Document doc = LogChunker.merge(Lists.newArrayList(message));
        final Map<String, Object> docMap = doc.toDocumentMap();
        final Map<String, String> fields = (Map<String, String>) docMap.get("fields");
        final Map<String, String> categories = (Map<String, String>) docMap.get("categories");

        assertThat(fields.get("text"), is("one two three"));
        assertThat(fields.get("ts"), is(ts.toString()));
        assertThat(categories.get("level"), is(Level.DEBUG.toString()));
    }

    @Test
    public void testConvertBatchToDocuments() {
        final Long ts = DateTime.now().getMillis();
        final LoggingProtos.BatchLogMessage.Builder batch = LoggingProtos.BatchLogMessage.newBuilder();
        final LoggingProtos.LogMessage message = LoggingProtos.LogMessage.newBuilder()
                .setMessage("ONE TWO THREE")
                .setLevel(Level.DEBUG.toInteger())
                .setOrigin("test")
                .setProduction(false)
                .setTs(ts).build();
        final LoggingProtos.LogMessage message2 = LoggingProtos.LogMessage.newBuilder()
                .setMessage("ONE TWO THREE")
                .setLevel(Level.WARN.toInteger())
                .setOrigin("test")
                .setProduction(false)
                .setTs(ts).build();
        final LoggingProtos.LogMessage message3 = LoggingProtos.LogMessage.newBuilder()
                .setMessage("ONE TWO THREE")
                .setLevel(Level.DEBUG.toInteger())
                .setOrigin("test")
                .setProduction(false)
                .setTs(ts).build();

        batch.addMessages(message).addMessages(message2).addMessages(message3);

        final List<IndexTankClient.Document> docs = LogChunker.chunkBatchLogMessage(batch.build());
        assertThat(docs.size(), is(batch.getMessagesCount()));
    }
}

package com.hello.suripu.core.actions;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by ksg on 1/30/17
 */
public class ActionProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(ActionProcessor.class);

    private static final Integer MAX_BUFFER_SIZE = 100;
    private final ActionFirehoseDAO firehoseDAO;
    private final List<Action> buffer;
    private int bufferSize = 0;

    public ActionProcessor(final ActionFirehoseDAO firehoseDAO) {
        this.firehoseDAO = firehoseDAO;
        this.buffer = Lists.newArrayListWithExpectedSize(MAX_BUFFER_SIZE);
    }

    public void addAction(final Action action) {
        this.buffer.add(action);
        final int currentBufferSize = buffer.size();
        if (currentBufferSize > MAX_BUFFER_SIZE) {
           final int added = this.firehoseDAO.batchInsertAll(buffer);
           if (added != currentBufferSize) {
               LOGGER.error("error=firehose-batch-inserts-incomplete inserted={} actual={}", added, currentBufferSize);
           } else {
               LOGGER.debug("action=firehose-batch-insert-success inserted={}", added);
           }
           buffer.clear();
        }
    }
}


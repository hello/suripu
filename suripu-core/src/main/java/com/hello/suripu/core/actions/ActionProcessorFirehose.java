package com.hello.suripu.core.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by ksg on 1/30/17
 */
public class ActionProcessorFirehose implements ActionProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(ActionProcessorFirehose.class);


    private final ActionFirehoseDAO firehoseDAO;
    private final Integer maxBufferSize;
    private final ConcurrentMap<Action, Long> buffer;

    private AtomicInteger inserted = new AtomicInteger(0);
    private AtomicInteger failed = new AtomicInteger(0);

    public ActionProcessorFirehose(final ActionFirehoseDAO firehoseDAO, final Integer maxBufferSize) {
        this.firehoseDAO = firehoseDAO;
        this.maxBufferSize = maxBufferSize;
        this.buffer = new ConcurrentHashMap<>();
    }

    @Override
    public Boolean add(final Action action) {
        final Long putResult = this.buffer.put(action, action.ts.getMillis());

        if (buffer.size() >= this.maxBufferSize) {
            flushToFirehose();
        }

        return putResult == null; // put returns null if key is previously absent
    }

    @Override
    public int bufferSize() { return buffer.size(); }

    @Override
    public int maxBufferSize() { return this.maxBufferSize; }

    /**
     * blocking writes to firehose
     */
    private synchronized void flushToFirehose() {
        final List<Action> actionList = new ArrayList<>(buffer.keySet());

        final int actionListSize = actionList.size();
        if (actionListSize < this.maxBufferSize) {
            LOGGER.debug("action=abort-flush reason=not-enough-data-in-buffer");
            return;
        }

        final int added = this.firehoseDAO.batchInsertAll(actionList);
        if (added != actionListSize) {
            LOGGER.error("error=firehose-batch-inserts-incomplete inserted={} actual={}", added, actionListSize);
            failed.addAndGet(actionListSize - added);
        } else {
            LOGGER.debug("action=firehose-batch-insert-success inserted={}", added);
        }
        inserted.addAndGet(added);

        // removed actions in the last batch
        for (final Action item : actionList) {
            buffer.remove(item);
        }
    }

}


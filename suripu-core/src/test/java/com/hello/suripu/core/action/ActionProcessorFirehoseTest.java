package com.hello.suripu.core.action;

import com.google.common.base.Optional;
import com.hello.suripu.core.actions.Action;
import com.hello.suripu.core.actions.ActionFirehoseDAO;
import com.hello.suripu.core.actions.ActionProcessor;
import com.hello.suripu.core.actions.ActionProcessorFirehose;
import com.hello.suripu.core.actions.ActionResult;
import com.hello.suripu.core.actions.ActionType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import javax.inject.Singleton;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Created by ksg on 2/2/17
 */
public class ActionProcessorFirehoseTest {
    private ActionFirehoseDAO firehoseDAO = mock(ActionFirehoseDAO.class);


    public class ActionRunnable implements Runnable {
        private final ActionProcessor processor;
        private final Integer numActions;
        private final ActionType actionType;

        ActionRunnable(final int numActions, final ActionProcessor processor, final ActionType actionType)
        {
            this.processor = processor;
            this.numActions = numActions;
            this.actionType = actionType;
        }

        public void run() {
            final DateTime now = DateTime.now(DateTimeZone.UTC);
            for (int i = 0; i < numActions; i++) {
                final Action action = new Action(1L, actionType, Optional.of(ActionResult.OKAY), now.plusMinutes(i), Optional.absent());
                processor.add(action);
            }
        }
    }

    @Test
    public void testAddSingleAction() {
        final ActionProcessor processor = new ActionProcessorFirehose(firehoseDAO);

        final Action action = new Action(1L, ActionType.LOGIN, Optional.of(ActionResult.OKAY), DateTime.now(DateTimeZone.UTC), Optional.absent());

        Boolean result = processor.add(action);
        assertThat(result, is(true));
        assertThat(processor.bufferSize(), is(1));

        result = processor.add(action);
        assertThat(result, is(false));
        assertThat(processor.bufferSize(), is(1));
    }

    @Test
    public void addManyActions() {
        final int extra = 10;
        final int numActions = ActionProcessorFirehose.MAX_BUFFER_SIZE + (2 * extra);
        final ActionProcessor processor = new ActionProcessorFirehose(firehoseDAO);
        final DateTime now = DateTime.now(DateTimeZone.UTC).minusHours(3);
        Boolean result = false;
        for (int i = 0; i < numActions; i++ ) {
            final Action action = new Action(1L, ActionType.LOGIN, Optional.of(ActionResult.OKAY), now.plusMinutes(i), Optional.absent());
            result = processor.add(action);

            if (i == ActionProcessorFirehose.MAX_BUFFER_SIZE - 2) {
                assertThat(result, is(true));
                assertThat(processor.bufferSize(), is(ActionProcessorFirehose.MAX_BUFFER_SIZE - 1));
            } else if (i == (numActions - extra - 1)) {
                assertThat(result, is(true));
                assertThat(processor.bufferSize(), is(extra));
            }
        }
        assertThat(result, is(true));
        assertThat(processor.bufferSize(), is(2*extra));
    }

    @Test
    public void multiThreadedAddActions() throws InterruptedException {
        @Singleton
        final ActionProcessor processor = new ActionProcessorFirehose(firehoseDAO);

        final CountDownLatch latch = new CountDownLatch(10000);
        final int numActions1 = 120;
        final int numActions2 = 150;

        final Thread thread1 = new Thread(new ActionRunnable(numActions1, processor, ActionType.LOGIN));
        final Thread thread2 = new Thread(new ActionRunnable(numActions2, processor, ActionType.TRENDS));

        thread1.start();
        latch.countDown();
        thread2.start();

        while (thread1.isAlive()) {
            thread1.join();
        }

        while (thread2.isAlive()) {
            thread2.join();
        }

        assertThat(processor.bufferSize() > 0, is(true));

    }
}

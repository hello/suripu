package com.hello.suripu.core.action;

import com.google.common.base.Optional;
import com.hello.suripu.core.actions.Action;
import com.hello.suripu.core.actions.ActionProcessor;
import com.hello.suripu.core.actions.ActionProcessorLog;
import com.hello.suripu.core.actions.ActionResult;
import com.hello.suripu.core.actions.ActionType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by ksg on 2/2/17
 */
public class ActionProcessorLogTest {

    @Test
    public void testAddActions() {

        final ActionProcessor processor = new ActionProcessorLog(2);
        final Action action = new Action(1L, ActionType.LOGIN, Optional.of(ActionResult.OKAY.string()), DateTime.now(DateTimeZone.UTC), Optional.absent());

        Boolean result = processor.add(action);
        assertThat(result, is(true));
        assertThat(processor.bufferSize(), is(1));

        // duplicate action, should not be added
        result = processor.add(action);
        assertThat(result, is(false));
        assertThat(processor.bufferSize(), is(1));

        // add a brand new action
        result = processor.add(new Action(2L, ActionType.LOGIN, Optional.of(ActionResult.OKAY.string()), DateTime.now(DateTimeZone.UTC), Optional.absent()));
        assertThat(result, is(true));

        // max buffer-size set to 2, so the buffer should be flushed
        assertThat(processor.bufferSize(), is(0));

    }

}

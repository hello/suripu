package com.hello.suripu.queue.workers;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.processors.TimelineProcessor;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Created by kingshy on 1/11/16.
 */
public class TimelineGenerator implements Callable<Optional<TimelineQueueProcessor.TimelineMessage>> {
    final private TimelineProcessor timelineProcessor;
    final private TimelineQueueProcessor.TimelineMessage message;

    public TimelineGenerator(TimelineProcessor timelineProcessor, TimelineQueueProcessor.TimelineMessage message) {
        this.timelineProcessor = timelineProcessor;
        this.message = message;
    }

    @Override
    public Optional<TimelineQueueProcessor.TimelineMessage> call() throws Exception {
        final TimelineProcessor newTimelineProcessor = timelineProcessor.copyMeWithNewUUID(UUID.randomUUID());
        final TimelineResult result = newTimelineProcessor.retrieveTimelinesFast(message.accountId, message.targetDate, Optional.<TimelineFeedback>absent());
        if (!result.getTimelineLogV2().isEmpty()) {
            return Optional.of(message);
        }
        return Optional.absent();
    }
}


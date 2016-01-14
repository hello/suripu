package com.hello.suripu.queue.workers;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.processors.TimelineProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Created by kingshy on 1/11/16.
 */
public class TimelineGenerator implements Callable<Optional<TimelineQueueProcessor.TimelineMessage>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineGenerator.class);

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
            final Timeline timeline = result.timelines.get(0);
            if (timeline.score > 0) {
                LOGGER.debug("account {}, date {}, score {}", message.accountId, message.targetDate, timeline.score);
            } else {
                LOGGER.debug("account {}, date {}, NO SCORE!", message.accountId, message.targetDate);
            }
            return Optional.of(message);
        }
        return Optional.absent();
    }
}


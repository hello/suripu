package com.hello.suripu.coredropwizard.timeline;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.processors.FeatureFlippedProcessor;
import org.joda.time.DateTime;

import java.util.UUID;

/**
 * Created by jarredheinrich on 4/21/17.
 */
public class TimelineProcessor extends FeatureFlippedProcessor {

    public final InstrumentedTimelineProcessor instrumentedTimelineProcessor;
    public final InstrumentedTimelineProcessorV3 instrumentedTimelineProcessorV3;

    private  TimelineProcessor(final InstrumentedTimelineProcessor instrumentedTimelineProcessor, final InstrumentedTimelineProcessorV3 instrumentedTimelineProcessorV3, final Optional<UUID> uuidOptional) {
        if(uuidOptional.isPresent()) {
            this.instrumentedTimelineProcessor = instrumentedTimelineProcessor.copyMeWithNewUUID(uuidOptional.get());
            this.instrumentedTimelineProcessorV3 = instrumentedTimelineProcessorV3.copyMeWithNewUUID(uuidOptional.get());
        } else{
            this.instrumentedTimelineProcessor = instrumentedTimelineProcessor;
            this.instrumentedTimelineProcessorV3 = instrumentedTimelineProcessorV3;
        }
    }
    public static TimelineProcessor  createTimelineProcessors(final InstrumentedTimelineProcessor instrumentedTimelineProcessor, final InstrumentedTimelineProcessorV3 instrumentedTimelineProcessorV3){
        return new TimelineProcessor(instrumentedTimelineProcessor, instrumentedTimelineProcessorV3, Optional.absent());
    }

    public TimelineProcessor  copyMeWithNewUUID(final UUID uuid){
        return new TimelineProcessor(instrumentedTimelineProcessor, instrumentedTimelineProcessorV3, Optional.of(uuid));
    }

    public TimelineResult retrieveTimelinesFast(final Long accountId, final DateTime queryDate, final Optional<Integer> queryHourOptional, final Optional<TimelineFeedback> newFeedback) {
        if (hasTimelineProcessorV3(accountId) && queryHourOptional.isPresent()) {
            return instrumentedTimelineProcessorV3.retrieveTimelinesFast(accountId, queryDate, queryHourOptional, newFeedback);
        }
        return instrumentedTimelineProcessor.retrieveTimelinesFast(accountId, queryDate, queryHourOptional, newFeedback);
    }

}

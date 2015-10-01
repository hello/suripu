package com.hello.suripu.workers.logs.timeline;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;

public class TimelineLogProcessorFactory implements IRecordProcessorFactory {

    private final TimelineAnalytics timelineAnalytics;

    public TimelineLogProcessorFactory(final TimelineAnalytics timelineAnalytics) {
        this.timelineAnalytics = timelineAnalytics;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return TimelineLogProcessor.create(timelineAnalytics);
    }
}

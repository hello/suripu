package com.hello.suripu.workers.logs.timeline;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.TimelineAnalyticsDAO;

public class TimelineLogProcessorFactory implements IRecordProcessorFactory {

    private final TimelineAnalyticsDAO timelineAnalyticsDAO;

    public TimelineLogProcessorFactory(final TimelineAnalyticsDAO timelineAnalyticsDAO) {
        this.timelineAnalyticsDAO = timelineAnalyticsDAO;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return TimelineLogProcessor.create(timelineAnalyticsDAO);
    }
}

package com.hello.suripu.workers.timeline;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.processors.TimelineProcessor;

/**
 * Created by pangwu on 9/23/14.
 */
public class TimelineRecordProcessorFactory implements IRecordProcessorFactory {

    private final TimelineProcessor timelineProcessor;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final TimelineWorkerConfiguration configuration;
    private final TimelineDAODynamoDB timelineDAODynamoDB;
    private final DeviceDAO deviceDAO;

    public TimelineRecordProcessorFactory(final TimelineProcessor timelineProcessor,
                                          final DeviceDAO deviceDAO,
                                          final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                          final TimelineDAODynamoDB timelineDAODynamoDB,
                                          final TimelineWorkerConfiguration configuration) {
        this.timelineProcessor = timelineProcessor;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.configuration = configuration;
        this.timelineDAODynamoDB = timelineDAODynamoDB;
        this.deviceDAO = deviceDAO;
    }


    @Override
    public IRecordProcessor createProcessor() {
        return new TimelineRecordProcessor(this.timelineProcessor,
                this.deviceDAO,
                this.mergedUserInfoDynamoDB,
                this.timelineDAODynamoDB,
                this.configuration);
    }
}

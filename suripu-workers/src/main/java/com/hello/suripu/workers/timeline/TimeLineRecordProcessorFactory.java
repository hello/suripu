package com.hello.suripu.workers.timeline;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.processors.TimelineProcessor;

/**
 * Created by pangwu on 9/23/14.
 */
public class TimeLineRecordProcessorFactory implements IRecordProcessorFactory {

    private final TimelineProcessor timelineProcessor;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final TimeLineWorkerConfiguration configuration;
    private final KeyStore pillKeyStore;

    public TimeLineRecordProcessorFactory(final TimelineProcessor timelineProcessor,
                                          final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                          final RingTimeDAODynamoDB ringTimeDAODynamoDB,
                                          final KeyStore pillKeyStore,
                                          final TimeLineWorkerConfiguration configuration) {
        this.timelineProcessor = timelineProcessor;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.configuration = configuration;
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;
        this.pillKeyStore = pillKeyStore;
    }


    @Override
    public IRecordProcessor createProcessor() {
        return new TimeLineRecordProcessor(this.timelineProcessor,
                this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.pillKeyStore,
                this.configuration);
    }
}

package com.hello.suripu.workers.alarm;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmRecordProcessorFactory implements IRecordProcessorFactory {

    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AlarmWorkerConfiguration configuration;


    public AlarmRecordProcessorFactory(
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final RingTimeDAODynamoDB ringTimeDAODynamoDB,
            final TrackerMotionDAO trackerMotionDAO,
            final AlarmWorkerConfiguration configuration) {

        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;
        this.configuration = configuration;
    }


    @Override
    public IRecordProcessor createProcessor() {
        return new AlarmRecordProcessor(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.configuration);
    }
}

package com.hello.suripu.workers.alarm;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.ScheduledRingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmRecordProcessorFactory implements IRecordProcessorFactory {

    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AlarmWorkerConfiguration configuration;


    public AlarmRecordProcessorFactory(
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB,
            final TrackerMotionDAO trackerMotionDAO,
            final AlarmWorkerConfiguration configuration) {

        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.scheduledRingTimeHistoryDAODynamoDB = scheduledRingTimeHistoryDAODynamoDB;
        this.configuration = configuration;
    }


    @Override
    public IRecordProcessor createProcessor() {
        return new AlarmRecordProcessor(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.configuration);
    }
}

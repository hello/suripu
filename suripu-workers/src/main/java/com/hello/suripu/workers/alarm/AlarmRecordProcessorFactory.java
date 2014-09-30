package com.hello.suripu.workers.alarm;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmRecordProcessorFactory implements IRecordProcessorFactory {

    private final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AlarmWorkerConfiguration configuration;


    public AlarmRecordProcessorFactory(
            final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB,
            final RingTimeDAODynamoDB ringTimeDAODynamoDB,
            final TrackerMotionDAO trackerMotionDAO,
            final AlarmWorkerConfiguration configuration) {

        this.mergedAlarmInfoDynamoDB = mergedAlarmInfoDynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;
        this.configuration = configuration;
    }


    @Override
    public IRecordProcessor createProcessor() {
        return new AlarmRecordProcessor(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.configuration);
    }
}

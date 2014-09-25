package com.hello.suripu.workers.alarm;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmRecordProcessorFactory implements IRecordProcessorFactory {

    private final AlarmDAODynamoDB alarmDAODynamoDB;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDAO deviceDAO;
    private final AlarmWorkerConfiguration configuration;


    public AlarmRecordProcessorFactory(
            final AlarmDAODynamoDB alarmDAODynamoDB,
            final RingTimeDAODynamoDB ringTimeDAODynamoDB,
            final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB,
            final TrackerMotionDAO trackerMotionDAO,
            final DeviceDAO deviceDAO,
            final AlarmWorkerConfiguration configuration) {
        this.alarmDAODynamoDB = alarmDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.timeZoneHistoryDAODynamoDB = timeZoneHistoryDAODynamoDB;
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.configuration = configuration;
    }


    @Override
    public IRecordProcessor createProcessor() {
        return new AlarmRecordProcessor(this.alarmDAODynamoDB, this.ringTimeDAODynamoDB, this.timeZoneHistoryDAODynamoDB,
                this.trackerMotionDAO, this.deviceDAO, this.configuration);
    }
}

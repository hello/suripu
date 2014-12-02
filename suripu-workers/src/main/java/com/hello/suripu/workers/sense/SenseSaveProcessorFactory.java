package com.hello.suripu.workers.sense;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;

public class SenseSaveProcessorFactory implements IRecordProcessorFactory {
    private final DeviceDAO deviceDAO;
    private final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB;
    private final DeviceDataDAO deviceDataDAO;

    public SenseSaveProcessorFactory(
            final DeviceDAO deviceDAO,
            final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB,
            final DeviceDataDAO deviceDataDAO) {
        this.deviceDAO = deviceDAO;
        this.mergedAlarmInfoDynamoDB = mergedAlarmInfoDynamoDB;
        this.deviceDataDAO = deviceDataDAO;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SenseSaveProcessor(deviceDAO, mergedAlarmInfoDynamoDB, deviceDataDAO);
    }
}

package com.hello.suripu.workers.sense;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;

public class SenseSaveProcessorFactory implements IRecordProcessorFactory {
    private final DeviceDAO deviceDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final DeviceDataDAO deviceDataDAO;

    public SenseSaveProcessorFactory(
            final DeviceDAO deviceDAO,
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final SensorsViewsDynamoDB sensorsViewsDynamoDB,
            final DeviceDataDAO deviceDataDAO) {
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.deviceDataDAO = deviceDataDAO;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SenseSaveProcessor(deviceDAO, mergedUserInfoDynamoDB, deviceDataDAO, sensorsViewsDynamoDB);
    }
}

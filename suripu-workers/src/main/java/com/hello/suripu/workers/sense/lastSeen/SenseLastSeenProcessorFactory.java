package com.hello.suripu.workers.sense.lastSeen;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.WifiInfoDAO;

public class SenseLastSeenProcessorFactory implements IRecordProcessorFactory {
    private final Integer maxRecords;
    private final WifiInfoDAO wifiInfoDAO;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private DeviceDAO deviceDAO;
    private MergedUserInfoDynamoDB mergedUserInfoDynamoDB;

    public SenseLastSeenProcessorFactory(final Integer maxRecords,
                                         final WifiInfoDAO wifiInfoDAO,
                                         final SensorsViewsDynamoDB sensorsViewsDynamoDB,
                                         final DeviceDAO deviceDAO,
                                         final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
        this.maxRecords = maxRecords;
        this.wifiInfoDAO = wifiInfoDAO;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;

    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SenseLastSeenProcessor(maxRecords, wifiInfoDAO, sensorsViewsDynamoDB, deviceDAO, mergedUserInfoDynamoDB);
    }
}

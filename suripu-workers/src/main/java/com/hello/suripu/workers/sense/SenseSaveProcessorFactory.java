package com.hello.suripu.workers.sense;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.workers.utils.ActiveDevicesTracker;
import redis.clients.jedis.JedisPool;

public class SenseSaveProcessorFactory implements IRecordProcessorFactory {
    private final DeviceDAO deviceDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final DeviceDataDAO deviceDataDAO;
    private final JedisPool jedisPool;

    public SenseSaveProcessorFactory(
            final DeviceDAO deviceDAO,
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final SensorsViewsDynamoDB sensorsViewsDynamoDB,
            final DeviceDataDAO deviceDataDAO,
            final JedisPool jedisPool) {
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.deviceDataDAO = deviceDataDAO;
        this.jedisPool = jedisPool;
    }

    @Override
    public IRecordProcessor createProcessor() {
        final ActiveDevicesTracker activeDevicesTracker = new ActiveDevicesTracker(jedisPool);
        return new SenseSaveProcessor(deviceDAO, mergedUserInfoDynamoDB, deviceDataDAO, activeDevicesTracker, sensorsViewsDynamoDB);
    }
}

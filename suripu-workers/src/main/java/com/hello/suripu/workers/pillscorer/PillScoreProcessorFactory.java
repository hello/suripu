package com.hello.suripu.workers.pillscorer;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.workers.utils.ActiveDevicesTracker;
import redis.clients.jedis.JedisPool;

public class PillScoreProcessorFactory implements IRecordProcessorFactory {

    private SleepScoreDAO sleepScoreDAO;
    private int dateMinuteBucket;
    private int checkpointThreshold;

    private final KeyStore keyStore;
    private final DeviceDAO deviceDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final JedisPool jedisPool;

    public PillScoreProcessorFactory(
            final SleepScoreDAO sleepScoreDAO,
            final int dateMinuteBucket,
            final int checkpointThreshold,
            final KeyStore keyStore,
            final DeviceDAO deviceDAO,
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final JedisPool jedisPool) {
        this.sleepScoreDAO = sleepScoreDAO;
        this.dateMinuteBucket = dateMinuteBucket;
        this.checkpointThreshold = checkpointThreshold;
        this.keyStore = keyStore;
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.jedisPool = jedisPool;
    }

    @Override
    public IRecordProcessor createProcessor() {
        final ActiveDevicesTracker activeDevicesTracker = new ActiveDevicesTracker(jedisPool);
        return new PillScoreProcessor(sleepScoreDAO, dateMinuteBucket, checkpointThreshold, keyStore, deviceDAO, mergedUserInfoDynamoDB, activeDevicesTracker);
    }
}

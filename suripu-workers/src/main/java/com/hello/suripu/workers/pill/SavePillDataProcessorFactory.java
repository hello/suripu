package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;

public class SavePillDataProcessorFactory implements IRecordProcessorFactory {

    private final int batchSize;
    private final TrackerMotionDAO trackerMotionDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final KeyStore pillKeyStore;
    private final DeviceDAO deviceDAO;
    private final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB;

    public SavePillDataProcessorFactory(
            final TrackerMotionDAO trackerMotionDAO,
            final int batchSize,
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final KeyStore pillKeyStore,
            final DeviceDAO deviceDAO,
            final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.batchSize = batchSize;
        this.mergedUserInfoDynamoDB= mergedUserInfoDynamoDB;
        this.pillKeyStore = pillKeyStore;
        this.deviceDAO = deviceDAO;
        this.pillHeartBeatDAODynamoDB = pillHeartBeatDAODynamoDB;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SavePillDataProcessor(trackerMotionDAO, batchSize, pillKeyStore, deviceDAO, mergedUserInfoDynamoDB, pillHeartBeatDAODynamoDB);
    }
}

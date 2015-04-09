package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.workers.utils.ActiveDevicesTracker;

public class SavePillDataProcessorFactory implements IRecordProcessorFactory {

    private final int batchSize;
    private final TrackerMotionDAO trackerMotionDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final KeyStore pillKeyStore;
    private final PillHeartBeatDAO pillHeartBeatDAO;
    private final DeviceDAO deviceDAO;
    private final ActiveDevicesTracker activeDevicesTracker;

    public SavePillDataProcessorFactory(
            final TrackerMotionDAO trackerMotionDAO,
            final int batchSize,
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final PillHeartBeatDAO pillHeartBeatDAO,
            final KeyStore pillKeyStore,
            final DeviceDAO deviceDAO,
            final ActiveDevicesTracker activeDevicesTracker) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.batchSize = batchSize;
        this.mergedUserInfoDynamoDB= mergedUserInfoDynamoDB;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.pillKeyStore = pillKeyStore;
        this.deviceDAO = deviceDAO;
        this.activeDevicesTracker = activeDevicesTracker;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SavePillDataProcessor(trackerMotionDAO, batchSize, pillHeartBeatDAO, pillKeyStore, deviceDAO, mergedUserInfoDynamoDB, activeDevicesTracker);
    }
}

package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillDataIngestDAO;
import com.hello.suripu.core.db.PillProxDataDAODynamoDB;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;

public class SavePillDataProcessorFactory implements IRecordProcessorFactory {

    private final int batchSize;
    private final PillDataIngestDAO pillDataIngestDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final KeyStore pillKeyStore;
    private final DeviceDAO deviceDAO;
    private final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB;
    private final PillProxDataDAODynamoDB pillProxDataDAODynamoDB;
    private final Boolean savePillHeartBeat;

    public SavePillDataProcessorFactory(
            final PillDataIngestDAO pillDataIngestDAO,
            final int batchSize,
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
            final KeyStore pillKeyStore,
            final DeviceDAO deviceDAO,
            final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB,
            final PillProxDataDAODynamoDB pillProxDataDAODynamoDB,
            final Boolean savePillHeartBeat) {
        this.pillDataIngestDAO = pillDataIngestDAO;
        this.batchSize = batchSize;
        this.mergedUserInfoDynamoDB= mergedUserInfoDynamoDB;
        this.pillKeyStore = pillKeyStore;
        this.deviceDAO = deviceDAO;
        this.pillHeartBeatDAODynamoDB = pillHeartBeatDAODynamoDB;
        this.pillProxDataDAODynamoDB = pillProxDataDAODynamoDB;
        this.savePillHeartBeat = savePillHeartBeat;

    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SavePillDataProcessor(pillDataIngestDAO, batchSize, pillKeyStore, deviceDAO, mergedUserInfoDynamoDB, pillHeartBeatDAODynamoDB, null, savePillHeartBeat);
    }
}

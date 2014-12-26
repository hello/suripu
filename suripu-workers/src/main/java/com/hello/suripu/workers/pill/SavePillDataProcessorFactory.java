package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;

public class SavePillDataProcessorFactory implements IRecordProcessorFactory {

    private final int batchSize;
    private final TrackerMotionDAO trackerMotionDAO;
    private final KinesisClientLibConfiguration configuration;
    private final KeyStore pillKeyStore;
    private final PillHeartBeatDAO pillHeartBeatDAO;

    public SavePillDataProcessorFactory(
            final TrackerMotionDAO trackerMotionDAO,
            final int batchSize,
            final KinesisClientLibConfiguration configuration,
            final PillHeartBeatDAO pillHeartBeatDAO,
            final KeyStore pillKeyStore) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.batchSize = batchSize;
        this.configuration = configuration;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.pillKeyStore = pillKeyStore;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SavePillDataProcessor(trackerMotionDAO, batchSize, pillHeartBeatDAO, pillKeyStore);
    }
}

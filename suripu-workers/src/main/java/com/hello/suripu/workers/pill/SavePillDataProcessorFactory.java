package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.hello.suripu.core.db.TrackerMotionDAO;

public class SavePillDataProcessorFactory implements IRecordProcessorFactory {

    private final int batchSize;
    private final TrackerMotionDAO trackerMotionDAO;
    private final KinesisClientLibConfiguration configuration;

    public SavePillDataProcessorFactory(
            final TrackerMotionDAO trackerMotionDAO,
            final int batchSize,
            final KinesisClientLibConfiguration configuration) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.batchSize = batchSize;
        this.configuration = configuration;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new SavePillDataProcessor(trackerMotionDAO, batchSize);
    }
}

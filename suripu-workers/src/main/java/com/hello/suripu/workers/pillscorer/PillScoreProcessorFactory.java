package com.hello.suripu.workers.pillscorer;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.hello.suripu.core.db.SleepScoreDAO;

public class PillScoreProcessorFactory implements IRecordProcessorFactory {

    private SleepScoreDAO sleepScoreDAO;
    private int dateMinuteBucket;
    private int checkpointThreshold;
    private final KinesisClientLibConfiguration configuration;

    public PillScoreProcessorFactory(
            final SleepScoreDAO sleepScoreDAO,
            final int dateMinuteBucket,
            final int checkpointThreshold,
            final KinesisClientLibConfiguration configuration) {
        this.sleepScoreDAO = sleepScoreDAO;
        this.dateMinuteBucket = dateMinuteBucket;
        this.checkpointThreshold = checkpointThreshold;
        this.configuration = configuration;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new PillScoreProcessor(sleepScoreDAO, dateMinuteBucket, checkpointThreshold);
    }
}

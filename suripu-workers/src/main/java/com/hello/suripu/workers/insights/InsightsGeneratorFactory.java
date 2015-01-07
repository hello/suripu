package com.hello.suripu.workers.insights;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.processors.insights.LightData;

/**
 * Created by kingshy on 1/6/15.
 */
public class InsightsGeneratorFactory implements IRecordProcessorFactory {
    private final AccountDAO accountDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final LightData lightData;

    public InsightsGeneratorFactory(final AccountDAO accountDAO,
                                    final DeviceDataDAO deviceDataDAO,
                                    final DeviceDAO deviceDAO,
                                    final TrackerMotionDAO trackerMotionDAO,
                                    final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                                    final InsightsDAODynamoDB insightsDAODynamoDB,
                                    final LightData lightData) {
        this.accountDAO = accountDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.lightData = lightData;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new InsightsGenerator(accountDAO, deviceDataDAO, deviceDAO, trackerMotionDAO, scoreDAODynamoDB, insightsDAODynamoDB, lightData);
    }
}

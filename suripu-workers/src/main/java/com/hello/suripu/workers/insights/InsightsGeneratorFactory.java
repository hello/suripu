package com.hello.suripu.workers.insights;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
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
    private final TrendsInsightsDAO trendsInsightsDAO;
    final QuestionResponseDAO questionResponseDAO;
    private final LightData lightData;

    public InsightsGeneratorFactory(final AccountDAO accountDAO,
                                    final DeviceDataDAO deviceDataDAO,
                                    final DeviceDAO deviceDAO,
                                    final TrackerMotionDAO trackerMotionDAO,
                                    final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                                    final InsightsDAODynamoDB insightsDAODynamoDB,
                                    final TrendsInsightsDAO trendsInsightsDAO,
                                    final QuestionResponseDAO questionResponseDAO,
                                    final LightData lightData) {
        this.accountDAO = accountDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.questionResponseDAO = questionResponseDAO;
        this.lightData = lightData;
    }

    @Override
    public IRecordProcessor createProcessor() {
        final AccountInfoProcessor.Builder builder = new AccountInfoProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withMapping(questionResponseDAO);
        final AccountInfoProcessor accountInfoProcessor = builder.build();

        final InsightProcessor.Builder insightBuilder = new InsightProcessor.Builder()
                .withSenseDAOs(deviceDataDAO, deviceDAO)
                .withTrackerMotionDAOs(trackerMotionDAO)
                .withInsightsDAOs(trendsInsightsDAO)
                .withDynamoDBDAOs(scoreDAODynamoDB, insightsDAODynamoDB)
                .withAccountInfoProcessor(accountInfoProcessor)
                .withLightData(lightData);

        final InsightProcessor insightProcessor = insightBuilder.build();

        return new InsightsGenerator(accountDAO, deviceDAO, insightProcessor);
    }
}

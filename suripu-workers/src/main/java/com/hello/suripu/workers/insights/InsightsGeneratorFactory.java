package com.hello.suripu.workers.insights;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.WakeStdDevData;

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
    private final QuestionResponseDAO questionResponseDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final LightData lightData;
    private final WakeStdDevData wakeStdDevData;
    private final AccountPreferencesDAO accountPreferencesDAO;

    public InsightsGeneratorFactory(final AccountDAO accountDAO,
                                    final DeviceDataDAO deviceDataDAO,
                                    final DeviceDAO deviceDAO,
                                    final TrackerMotionDAO trackerMotionDAO,
                                    final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                                    final InsightsDAODynamoDB insightsDAODynamoDB,
                                    final TrendsInsightsDAO trendsInsightsDAO,
                                    final QuestionResponseDAO questionResponseDAO,
                                    final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                                    final LightData lightData,
                                    final WakeStdDevData wakeStdDevData,
                                    final AccountPreferencesDAO accountPreferencesDAO) {
        this.accountDAO = accountDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.questionResponseDAO = questionResponseDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.lightData = lightData;
        this.wakeStdDevData = wakeStdDevData;
        this.accountPreferencesDAO = accountPreferencesDAO;
    }

    @Override
    public IRecordProcessor createProcessor() {
        final AccountInfoProcessor.Builder builder = new AccountInfoProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withMapping(questionResponseDAO);
        final AccountInfoProcessor accountInfoProcessor = builder.build();

        final InsightProcessor.Builder insightBuilder = new InsightProcessor.Builder()
                .withSenseDAOs(deviceDataDAO, deviceDAO)
                .withTrackerMotionDAO(trackerMotionDAO)
                .withInsightsDAO(trendsInsightsDAO)
                .withDynamoDBDAOs(scoreDAODynamoDB, insightsDAODynamoDB)
                .withSleepStatsDAODynamoDB(sleepStatsDAODynamoDB)
                .withAccountInfoProcessor(accountInfoProcessor)
                .withLightData(lightData)
                .withWakeStdDevData(wakeStdDevData)
                .withPreferencesDAO(accountPreferencesDAO);

        final InsightProcessor insightProcessor = insightBuilder.build();

        return new InsightsGenerator(accountDAO, deviceDAO, insightProcessor);
    }
}

package com.hello.suripu.workers.insights;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
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
    private final AccountReadDAO accountDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private final DeviceReadDAO deviceDAO;
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final TrendsInsightsDAO trendsInsightsDAO;
    private final QuestionResponseReadDAO questionResponseDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final LightData lightData;
    private final WakeStdDevData wakeStdDevData;
    private final AccountPreferencesDAO accountPreferencesDAO;
    private final CalibrationDAO calibrationDAO;

    public InsightsGeneratorFactory(final AccountDAO accountDAO,
                                    final DeviceDataDAO deviceDataDAO,
                                    final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                                    final DeviceDAO deviceDAO,
                                    final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB,
                                    final TrackerMotionDAO trackerMotionDAO,
                                    final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                                    final InsightsDAODynamoDB insightsDAODynamoDB,
                                    final TrendsInsightsDAO trendsInsightsDAO,
                                    final QuestionResponseReadDAO questionResponseDAO,
                                    final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                                    final LightData lightData,
                                    final WakeStdDevData wakeStdDevData,
                                    final AccountPreferencesDAO accountPreferencesDAO,
                                    final CalibrationDAO calibrationDAO) {
        this.accountDAO = accountDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.timeZoneHistoryDAODynamoDB = timeZoneHistoryDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.questionResponseDAO = questionResponseDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.lightData = lightData;
        this.wakeStdDevData = wakeStdDevData;
        this.accountPreferencesDAO = accountPreferencesDAO;
        this.calibrationDAO = calibrationDAO;
    }

    @Override
    public IRecordProcessor createProcessor() {
        final AccountInfoProcessor.Builder builder = new AccountInfoProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withMapping(questionResponseDAO);
        final AccountInfoProcessor accountInfoProcessor = builder.build();

        final InsightProcessor.Builder insightBuilder = new InsightProcessor.Builder()
                .withSenseDAOs(deviceDataDAO, deviceDataDAODynamoDB, deviceDAO)
                .withTimeZoneHistoryDAO(timeZoneHistoryDAODynamoDB)
                .withTrackerMotionDAO(trackerMotionDAO)
                .withInsightsDAO(trendsInsightsDAO)
                .withDynamoDBDAOs(scoreDAODynamoDB, insightsDAODynamoDB, sleepStatsDAODynamoDB)
                .withAccountInfoProcessor(accountInfoProcessor)
                .withLightData(lightData)
                .withWakeStdDevData(wakeStdDevData)
                .withPreferencesDAO(accountPreferencesDAO)
                .withCalibrationDAO(calibrationDAO);

        final InsightProcessor insightProcessor = insightBuilder.build();

        return new InsightsGenerator(accountDAO, deviceDAO, insightProcessor);
    }
}

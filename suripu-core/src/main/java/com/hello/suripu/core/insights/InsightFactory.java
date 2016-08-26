package com.hello.suripu.core.insights;

import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.MarketingInsightsSeenDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.insights.models.Dummy;
import com.hello.suripu.core.insights.models.InsightModel;
import com.hello.suripu.core.insights.models.SleepAlarm;
import com.hello.suripu.core.insights.models.WakeVariance;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.processors.AccountInfoProcessor;

public class InsightFactory {

    private final DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private final DeviceReadDAO deviceReadDAO;
    private final TrendsInsightsDAO trendsInsightsDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final InsightsLastSeenDAO insightsLastSeenDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final AccountPreferencesDAO preferencesDAO;
    private final AccountInfoProcessor accountInfoProcessor;
    private final AccountReadDAO accountReadDAO;
    private final CalibrationDAO calibrationDAO;
    private final MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB;

    public InsightFactory(DeviceDataDAODynamoDB deviceDataDAODynamoDB, DeviceReadDAO deviceReadDAO, TrendsInsightsDAO trendsInsightsDAO, AggregateSleepScoreDAODynamoDB scoreDAODynamoDB, InsightsDAODynamoDB insightsDAODynamoDB, InsightsLastSeenDAO insightsLastSeenDAO, SleepStatsDAODynamoDB sleepStatsDAODynamoDB, AccountPreferencesDAO preferencesDAO, AccountInfoProcessor accountInfoProcessor, AccountReadDAO accountReadDAO, CalibrationDAO calibrationDAO, MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB) {
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.deviceReadDAO = deviceReadDAO;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.insightsLastSeenDAO = insightsLastSeenDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.preferencesDAO = preferencesDAO;
        this.accountInfoProcessor = accountInfoProcessor;
        this.accountReadDAO = accountReadDAO;
        this.calibrationDAO = calibrationDAO;
        this.marketingInsightsSeenDAODynamoDB = marketingInsightsSeenDAODynamoDB;
    }

    public InsightModel wakeVariance() {
        return WakeVariance.create(sleepStatsDAODynamoDB);
    }

    public InsightModel sleepAlarm(){
        return SleepAlarm.create(sleepStatsDAODynamoDB,accountReadDAO,preferencesDAO);
    }


    public InsightModel fromCategory(InsightCard.Category category) {
        switch (category) {
            case WAKE_VARIANCE:
                return wakeVariance();
        }

        return new Dummy();
    }
}

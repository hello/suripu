package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.db.responses.DeviceDataResponse;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.librato.rollout.RolloutClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jyfan on 9/4/15.
 */
public class InsightProcessorTest {



    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final DeviceId FAKE_DEVICE_ID = DeviceId.create(9998L);

    private final DateTime FAKE_SATURDAY = DateTime.parse("2015-09-05").withTimeAtStartOfDay();
    private final DateTime FAKE_FRIDAY = DateTime.parse("2015-09-04").withTimeAtStartOfDay();

    private final DateTime FAKE_DATE_1 = DateTime.parse("2015-09-01").withTimeAtStartOfDay();
    private final DateTime FAKE_DATE_10 = DateTime.parse("2015-09-10").withTimeAtStartOfDay();
    private final DateTime FAKE_DATE_NONE = DateTime.parse("2015-09-11").withTimeAtStartOfDay();

    private DeviceDataDAO deviceDataDAO;

    private static RolloutClient featureFlipOn() {
        final Long FAKE_ACCOUNT_ID = 9999L;

        RolloutClient mockFeatureFlipper = Mockito.mock(RolloutClient.class);
        Mockito.when(mockFeatureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_HUMIDITY, FAKE_ACCOUNT_ID, Collections.EMPTY_LIST)).thenReturn(Boolean.TRUE);

        return mockFeatureFlipper;
    }

    private static RolloutClient featureFlipOff() {
        final Long FAKE_ACCOUNT_ID = 9999L;

        RolloutClient mockFeatureFlipper = Mockito.mock(RolloutClient.class);
        Mockito.when(mockFeatureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_HUMIDITY, FAKE_ACCOUNT_ID, Collections.EMPTY_LIST)).thenReturn(Boolean.FALSE);

        return mockFeatureFlipper;
    }

    private InsightProcessor setUp() {

        final Long FAKE_ACCOUNT_ID = 9999L;
        final Long FAKE_DEVICE_ID = 9998L;
        final Long FAKE_PILL_ID = 9997L;

        deviceDataDAO = Mockito.mock(DeviceDataDAO.class);
        final DeviceDataDAODynamoDB deviceDataDAODynamoDB = Mockito.mock(DeviceDataDAODynamoDB.class);
        final DeviceDAO deviceDAO = Mockito.mock(DeviceDAO.class);
        Mockito.when(deviceDAO.getMostRecentSenseByAccountId(FAKE_ACCOUNT_ID)).thenReturn(Optional.of(FAKE_DEVICE_ID));
        final TrendsInsightsDAO trendsInsightsDAO = Mockito.mock(TrendsInsightsDAO.class);
        final TrackerMotionDAO trackerMotionDAO = Mockito.mock(TrackerMotionDAO.class);
        final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB = Mockito.mock(AggregateSleepScoreDAODynamoDB.class);
        final InsightsDAODynamoDB insightsDAODynamoDB = Mockito.mock(InsightsDAODynamoDB.class);
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = Mockito.mock(SleepStatsDAODynamoDB.class);
        final AccountPreferencesDAO preferencesDAO = Mockito.mock(AccountPreferencesDAO.class);
        final LightData lightData = Mockito.mock(LightData.class);
        final WakeStdDevData wakeStdDevData = Mockito.mock(WakeStdDevData.class);
        final CalibrationDAO calibrationDAO = Mockito.mock(CalibrationDAO.class);
        final AccountInfoProcessor accountInfoProcessor = Mockito.mock(AccountInfoProcessor.class);

        //Prepping for taking care of @NotNull check for light
        final int light = 2;
        final int zeroLight = 0;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = Lists.newArrayList();
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withHourOfDay(21), offsetMillis, 1, 1, 1, 0, 0, 0));

        final List<InfoInsightCards> mockInfoInsightCardsList = Lists.newArrayList(Mockito.mock(InfoInsightCards.class));
        final DeviceStatus mockDeviceStatus = Mockito.mock(DeviceStatus.class);
        final AggregateScore mockAggregateScore = Mockito.mock(AggregateScore.class);
        final List<InsightCard> mockInsightCardList = Lists.newArrayList(Mockito.mock(InsightCard.class));

        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);
        final SleepStats fakeSleepStat = Mockito.mock(SleepStats.class);
        final List<AggregateSleepStats> fakeAggregateSleepStatsList = Lists.newArrayList();
        fakeAggregateSleepStatsList.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, timestamp, offsetMillis, 0, "1", fakeMotionScore, fakeSleepStat));

        //Taking care of @NotNull check for light
        final DeviceDataResponse successfulResponse = new DeviceDataResponse(ImmutableList.copyOf(data), Response.Status.SUCCESS, Optional.<Exception>absent());
        Mockito.when(deviceDataDAO.getLightByBetweenHourDateByTS(Mockito.any(Long.class), Mockito.any(DeviceId.class), Mockito.any(Integer.class), Mockito.any(DateTime.class), Mockito.any(DateTime.class), Mockito.any(DateTime.class), Mockito.any(DateTime.class),Mockito.any(Integer.class), Mockito.any(Integer.class))).thenReturn(successfulResponse);

        Mockito.when(deviceDataDAO.toString()).thenReturn("someString");
        Mockito.when(deviceDAO.getMostRecentSenseByAccountId(FAKE_ACCOUNT_ID)).thenReturn(Optional.of(FAKE_DEVICE_ID));
        Mockito.when(trendsInsightsDAO.getAllGenericInsightCards()).thenReturn(ImmutableList.copyOf(mockInfoInsightCardsList));
        Mockito.when(trackerMotionDAO.pillStatus(FAKE_PILL_ID, FAKE_ACCOUNT_ID)).thenReturn(Optional.of(mockDeviceStatus));
        Mockito.when(scoreDAODynamoDB.getSingleScore(FAKE_ACCOUNT_ID, "2015-09-14")).thenReturn(mockAggregateScore);
        Mockito.when(insightsDAODynamoDB.getInsightsByCategory(FAKE_ACCOUNT_ID, InsightCard.Category.LIGHT, 1)).thenReturn(ImmutableList.copyOf(mockInsightCardList));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(Mockito.any(Long.class), Mockito.any(String.class), Mockito.any(String.class))).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList));

        Mockito.when(preferencesDAO.toString()).thenReturn("someString");
        Mockito.when(accountInfoProcessor.toString()).thenReturn("someString");
        Mockito.when(lightData.getLightPercentile(Mockito.any(Integer.class))).thenReturn(1);
        Mockito.when(wakeStdDevData.getWakeStdDevPercentile(Mockito.any(Integer.class))).thenReturn(1);

        //Taking care of @NotNull check for humidity
        Mockito.when(sleepStatsDAODynamoDB.getTimeZoneOffset(FAKE_ACCOUNT_ID)).thenReturn(Optional.of(offsetMillis));
        Mockito.when(deviceDataDAO.getBetweenHourDateByTS(Mockito.any(Long.class), Mockito.any(DeviceId.class),Mockito.any(DateTime.class), Mockito.any(DateTime.class), Mockito.any(DateTime.class), Mockito.any(DateTime.class), Mockito.any(Integer.class), Mockito.any(Integer.class)))
                .thenReturn(successfulResponse);
        Mockito.when(insightsDAODynamoDB.getInsightsByCategory(FAKE_ACCOUNT_ID, InsightCard.Category.HUMIDITY, 1)).thenReturn(ImmutableList.copyOf(mockInsightCardList));

        //Initialize InsightProcessor
        final InsightProcessor insightProcessor = new InsightProcessor(deviceDataDAO, deviceDataDAODynamoDB, deviceDAO,
                trendsInsightsDAO,
                trackerMotionDAO,
                scoreDAODynamoDB,
                insightsDAODynamoDB,
                sleepStatsDAODynamoDB,
                preferencesDAO,
                accountInfoProcessor,
                lightData,
                wakeStdDevData,
                calibrationDAO);

        //only to get rid of null pointer exception
        final InsightCard insightCardMock = Mockito.mock(InsightCard.class);
        final ImmutableList<InsightCard> insightCardMockList = ImmutableList.copyOf(Lists.newArrayList(insightCardMock));
        Mockito.when(insightsDAODynamoDB.getInsightsByDate(FAKE_ACCOUNT_ID, DateTime.now().minusDays(7), Boolean.TRUE, 7)).thenReturn(insightCardMockList);

        return insightProcessor;
    }

    @Test
    public void test_generateNewUserInsights() {
        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();

        final Optional<InsightCard.Category> something = spyInsightProcessor.generateNewUserInsights(FAKE_ACCOUNT_ID, 1, recentCategories);
        assertThat(something.get(), is(InsightCard.Category.GENERIC));
    }

    @Test
    public void test_generateNewUserInsights_2() {
        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.GENERIC);

        final Optional<InsightCard.Category> something = spyInsightProcessor.generateNewUserInsights(FAKE_ACCOUNT_ID, 1, recentCategories);
        assertThat(something.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_generateNewUserInsights_3() {
        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.GENERIC);

        final Optional<InsightCard.Category> something = spyInsightProcessor.generateNewUserInsights(FAKE_ACCOUNT_ID, 2, recentCategories);
        assertThat(something.get(), is(InsightCard.Category.SLEEP_HYGIENE));
    }

    @Test
    public void test_generateNewUserInsights_4() {
        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.GENERIC);

        final Optional<InsightCard.Category> something = spyInsightProcessor.generateNewUserInsights(FAKE_ACCOUNT_ID, 3, recentCategories);
        assertThat(something.get(), is(InsightCard.Category.SLEEP_DURATION));
    }

    @Test
    public void test_generateGeneralInsights() {

        final RolloutClient mockFeatureFlipper = featureFlipOff();
        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);
        recentCategories.add(InsightCard.Category.TEMPERATURE);

        spyInsightProcessor.generateGeneralInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, recentCategories, FAKE_SATURDAY, mockFeatureFlipper);

        //Tests - Look for weekly Insight, try to generate wake variance, get Optional.absent() b/c no data
        Mockito.verify(spyInsightProcessor).selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY);
        Mockito.verify(spyInsightProcessor).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.WAKE_VARIANCE);

        final Optional<InsightCard.Category> wakeCardCategory = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.WAKE_VARIANCE);
        assertThat(wakeCardCategory.isPresent(), is(Boolean.FALSE));

        //look for high priority Insight - get nothing
        Mockito.verify(spyInsightProcessor).selectHighPriorityInsightToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY, mockFeatureFlipper);
        Optional<InsightCard.Category> expectedHighPriority = insightProcessor.selectHighPriorityInsightToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY, mockFeatureFlipper);
        assertThat(expectedHighPriority.isPresent(), is(Boolean.FALSE));

        //look for random old Insight - get nothing b/c wrong date
        Mockito.verify(spyInsightProcessor).selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY, mockFeatureFlipper);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.LIGHT);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.TEMPERATURE);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.SLEEP_QUALITY);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.BED_LIGHT_DURATION);
    }

    @Test
    public void test_generateGeneralInsights_3() {

        final RolloutClient mockFeatureFlipper = featureFlipOn();
        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);
        recentCategories.add(InsightCard.Category.WAKE_VARIANCE);

        spyInsightProcessor.generateGeneralInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, recentCategories, FAKE_SATURDAY, mockFeatureFlipper);

        //Tests - Look for weekly Insight, do not try to generate b/c recent
        Mockito.verify(spyInsightProcessor).selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.WAKE_VARIANCE);

        final Optional<InsightCard.Category> wakeCardCategory = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.WAKE_VARIANCE);
        assertThat(wakeCardCategory.isPresent(), is(Boolean.FALSE));

        //look for high priority Insight - get nothing b/c feature wrong date
        Mockito.verify(spyInsightProcessor).selectHighPriorityInsightToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY, mockFeatureFlipper);

        //look for random old Insight - get nothing b/c wrong date
        Mockito.verify(spyInsightProcessor).selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY, mockFeatureFlipper);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.LIGHT);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.TEMPERATURE);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.SLEEP_QUALITY);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.BED_LIGHT_DURATION);
    }

    @Test
    public void test_generateGeneralInsights_5() {

        final RolloutClient mockFeatureFlipper = featureFlipOff();
        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
//        recentCategories.add(InsightCard.Category.LIGHT);
        recentCategories.add(InsightCard.Category.TEMPERATURE);
        recentCategories.add(InsightCard.Category.WAKE_VARIANCE);
//        recentCategories.add(InsightCard.Category.HUMIDITY);

        spyInsightProcessor.generateGeneralInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, recentCategories, FAKE_DATE_1, mockFeatureFlipper);

        //Tests - Look for weekly Insight, do not try to generate b/c wrong date
        Mockito.verify(spyInsightProcessor).selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.WAKE_VARIANCE);

        //look for high priority Insight - get nothing
        Mockito.verify(spyInsightProcessor).selectHighPriorityInsightToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1, mockFeatureFlipper);

        //look for random old Insight, do not try to generate humidity b/c featureFlip Off
        Mockito.verify(spyInsightProcessor).selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1, mockFeatureFlipper);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.HUMIDITY);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.TEMPERATURE);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.SLEEP_QUALITY);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.BED_LIGHT_DURATION);
    }

    @Test
    public void test_generateGeneralInsights_6() {

        final RolloutClient mockFeatureFlipper = featureFlipOn();
        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.TEMPERATURE);
        recentCategories.add(InsightCard.Category.LIGHT);

        spyInsightProcessor.generateGeneralInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, recentCategories, FAKE_DATE_1, mockFeatureFlipper);

        //Tests - Look for weekly Insight, do not try to generate b/c wrong date
        Mockito.verify(spyInsightProcessor).selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.WAKE_VARIANCE);

        //look for high priority Insight - get nothing
        Mockito.verify(spyInsightProcessor).selectHighPriorityInsightToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1, mockFeatureFlipper);

        //look for random old Insight, try to generate humidity
        Mockito.verify(spyInsightProcessor).selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1, mockFeatureFlipper);
        Mockito.verify(spyInsightProcessor).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.HUMIDITY);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.TEMPERATURE);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.SLEEP_QUALITY);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.BED_LIGHT_DURATION);
    }

    @Test
    public void test_generateCategory_light() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        final Optional<InsightCard.Category> generatedInsight = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.LIGHT);

        assertThat(generatedInsight.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void test_generateCategory_humidity() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        final Optional<InsightCard.Category> generatedInsight = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.HUMIDITY);

        assertThat(generatedInsight.isPresent(), is(Boolean.TRUE));
    }


    @Test
    public void test_generateCategory_wakeVariance() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        final Optional<InsightCard.Category> generatedInsight = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, InsightCard.Category.WAKE_VARIANCE);

        //no real data for wake variance, will not generate Insight
        assertThat(generatedInsight.isPresent(), is(Boolean.FALSE));
    }
}

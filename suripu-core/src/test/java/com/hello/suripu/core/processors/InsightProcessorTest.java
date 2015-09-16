package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jyfan on 9/4/15.
 */
public class InsightProcessorTest {

    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final Long FAKE_DEVICE_ID = 9998L;

    private final DateTime FAKE_SATURDAY = DateTime.parse("2015-09-05").withTimeAtStartOfDay();
    private final DateTime FAKE_FRIDAY = DateTime.parse("2015-09-04").withTimeAtStartOfDay();

    private final DateTime FAKE_DATE_1 = DateTime.parse("2015-09-01").withTimeAtStartOfDay();
    private final DateTime FAKE_DATE_10 = DateTime.parse("2015-09-10").withTimeAtStartOfDay();

    private static InsightProcessor setUp() {
        final Long FAKE_ACCOUNT_ID = 9999L;
        final Long FAKE_DEVICE_ID = 9998L;
        final Long FAKE_PILL_ID = 9997L;

        final DeviceDataDAO deviceDataDAO = Mockito.mock(DeviceDataDAO.class);
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
        final AccountInfoProcessor accountInfoProcessor = Mockito.mock(AccountInfoProcessor.class);
        final Map<String, String> insightInfoPreview =  Mockito.mock(HashMap.class);

        //Prepping for taking care of @NotNull check
        final Long accountId = FAKE_ACCOUNT_ID;
        final Long deviceId = FAKE_DEVICE_ID;
        final int light = 2;
        final int zeroLight = 0;
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
        final int offsetMillis = -28800000;
        final List<DeviceData> data = Lists.newArrayList();
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp, offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(10), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light + 1,light + 1, 0, 0, timestamp.withMinuteOfHour(30), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, light,light, 0, 0, timestamp.withMinuteOfHour(45), offsetMillis, 1, 1, 1, 0, 0, 0));
        data.add(new DeviceData(accountId, deviceId, 0, 0, 0, 0, 0, 0, 0, zeroLight,zeroLight, 0, 0, timestamp.withHourOfDay(21), offsetMillis, 1, 1, 1, 0, 0, 0));

        final List<InfoInsightCards> mockInfoInsightCardsList = Lists.newArrayList(Mockito.mock(InfoInsightCards.class));
        final DeviceStatus mockDeviceStatus = Mockito.mock(DeviceStatus.class);
        final AggregateScore mockAggregateScore = Mockito.mock(AggregateScore.class);
        final List<InsightCard> mockInsightCardList = Lists.newArrayList(Mockito.mock(InsightCard.class));

        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);
        final SleepStats fakeSleepStat = Mockito.mock(SleepStats.class);
        final List<AggregateSleepStats> fakeAggregateSleepStatsList = Lists.newArrayList();
        fakeAggregateSleepStatsList.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, timestamp, offsetMillis, 0, "1", fakeMotionScore, fakeSleepStat));

        //Taking care of @NotNull check
        Mockito.when(deviceDataDAO.getLightByBetweenHourDate(Mockito.any(Long.class), Mockito.any(Long.class), Mockito.any(Integer.class), Mockito.any(DateTime.class), Mockito.any(DateTime.class), Mockito.any(Integer.class), Mockito.any(Integer.class))).thenReturn(ImmutableList.copyOf(data));

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
        Mockito.when(insightInfoPreview.toString()).thenReturn("someString");

        //Initialize InsightProcessor
        final InsightProcessor insightProcessor = new InsightProcessor(deviceDataDAO, deviceDAO,
                trendsInsightsDAO,
                trackerMotionDAO,
                scoreDAODynamoDB,
                insightsDAODynamoDB,
                sleepStatsDAODynamoDB,
                preferencesDAO,
                accountInfoProcessor,
                lightData,
                wakeStdDevData,
                insightInfoPreview);

        //only to get rid of null pointer exception
        final InsightCard insightCardMock = Mockito.mock(InsightCard.class);
        final ImmutableList<InsightCard> insightCardMockList = ImmutableList.copyOf(Lists.newArrayList(insightCardMock));
        Mockito.when(insightsDAODynamoDB.getInsightsByDate(FAKE_ACCOUNT_ID, DateTime.now().minusDays(7), Boolean.TRUE, 7)).thenReturn(insightCardMockList);

        return insightProcessor;
    }

    @Test
    public void test_generateGeneralInsights() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);
        recentCategories.add(InsightCard.Category.TEMPERATURE);

        spyInsightProcessor.generateGeneralInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, recentCategories, FAKE_SATURDAY);

        //Tests - Look for weekly Insight, try to generate wake variance, get Optional.absent(), look for random Insight, try to generate nothing else
        assertThat(InsightProcessor.selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY), is(Optional.of(InsightCard.Category.WAKE_VARIANCE)));
        Mockito.verify(spyInsightProcessor).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.WAKE_VARIANCE);

        final Optional<InsightCard.Category> wakeCardCategory = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.WAKE_VARIANCE);
        assertThat(wakeCardCategory.isPresent(), is(Boolean.FALSE));

        assertThat(InsightProcessor.selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY).isPresent(), is(Boolean.FALSE));
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.LIGHT);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.TEMPERATURE);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.SLEEP_QUALITY);
    }

    @Test
    public void test_generateGeneralInsights_2() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);
        recentCategories.add(InsightCard.Category.TEMPERATURE);
        recentCategories.add(InsightCard.Category.SLEEP_QUALITY);

        spyInsightProcessor.generateGeneralInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, recentCategories, FAKE_SATURDAY);

        //Tests - Look for weekly Insight, try to generate wake variance, get Optional.absent(), do not look for random Insight, try to generate nothing else
        assertThat(InsightProcessor.selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY), is(Optional.of(InsightCard.Category.WAKE_VARIANCE)));
        Mockito.verify(spyInsightProcessor).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.WAKE_VARIANCE);

        final Optional<InsightCard.Category> wakeCardCategory = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.WAKE_VARIANCE);
        assertThat(wakeCardCategory.isPresent(), is(Boolean.FALSE));

        assertThat(InsightProcessor.selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY).isPresent(), is(Boolean.FALSE));
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.LIGHT);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.TEMPERATURE);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.SLEEP_QUALITY);
    }

    @Test
    public void test_generateGeneralInsights_3() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);
        recentCategories.add(InsightCard.Category.WAKE_VARIANCE);

        spyInsightProcessor.generateGeneralInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, recentCategories, FAKE_SATURDAY);

        //Tests - Look for weekly Insight, do not try to generate wake variance b/c recent, look for random Insight, try to generate nothing else
        assertThat(InsightProcessor.selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY).isPresent(), is(Boolean.FALSE));
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.WAKE_VARIANCE);

        final Optional<InsightCard.Category> wakeCardCategory = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.WAKE_VARIANCE);
        assertThat(wakeCardCategory.isPresent(), is(Boolean.FALSE));

        assertThat(InsightProcessor.selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY).isPresent(), is(Boolean.FALSE));
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.LIGHT);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.TEMPERATURE);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.SLEEP_QUALITY);
    }

    @Test
    public void test_generateGeneralInsights_4() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        //actually simulating recent categories
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.TEMPERATURE);
        recentCategories.add(InsightCard.Category.SLEEP_QUALITY);

        spyInsightProcessor.generateGeneralInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, recentCategories, FAKE_DATE_1);

        //Tests - Look for weekly Insight, do not try to generate wake variance wrong day, look for random Insight, try to generate light and nothing else
        assertThat(InsightProcessor.selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1).isPresent(), is(Boolean.FALSE));
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.WAKE_VARIANCE);

        assertThat(InsightProcessor.selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1), is(Optional.of(InsightCard.Category.LIGHT)));
        Mockito.verify(spyInsightProcessor).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.LIGHT);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.TEMPERATURE);
        Mockito.verify(spyInsightProcessor, Mockito.never()).generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.SLEEP_QUALITY);
    }

    @Test
    public void test_generateCategory_light() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        final Optional<InsightCard.Category> generatedInsight = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.LIGHT);

        assertThat(generatedInsight.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void test_generateCategory_wakeVariance() {

        final InsightProcessor insightProcessor = setUp();
        final InsightProcessor spyInsightProcessor = Mockito.spy(insightProcessor);

        final Optional<InsightCard.Category> generatedInsight = spyInsightProcessor.generateInsightsByCategory(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, InsightCard.Category.WAKE_VARIANCE);

        //no real data for wake variance, will not generate Insight
        assertThat(generatedInsight.isPresent(), is(Boolean.FALSE));
    }


    @Test
    public void test_generateWeeklyInsights_no() {
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.WAKE_VARIANCE);

        final Optional<InsightCard.Category> insightToGenerate = InsightProcessor.selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY);
        assertThat(insightToGenerate.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_generateWeeklyInsights_no2() {
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);

        final Optional<InsightCard.Category> insightToGenerate = InsightProcessor.selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_FRIDAY);
        assertThat(insightToGenerate.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_generateWeeklyInsights_light() {
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);

        final Optional<InsightCard.Category> insightToGenerate = InsightProcessor.selectWeeklyInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_SATURDAY);
        assertThat(insightToGenerate.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void test_generateRandomOldInsight_no() {
        //correct date, but light has already been generated
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);

        final Optional<InsightCard.Category> insightToGenerate = InsightProcessor.selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1);
        assertThat(insightToGenerate.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_generateRandomOldInsight_light() {
        //correct date & light not recently generated
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.TEMPERATURE);

        final Optional<InsightCard.Category> insightToGenerate = InsightProcessor.selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_1);
        assertThat(insightToGenerate.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void test_generateRandomOldInsight_temp() {
        //correct date & temp not recently generated
        final Set<InsightCard.Category> recentCategories = new HashSet<>();
        recentCategories.add(InsightCard.Category.LIGHT);

        final Optional<InsightCard.Category> insightToGenerate = InsightProcessor.selectRandomOldInsightsToGenerate(FAKE_ACCOUNT_ID, recentCategories, FAKE_DATE_10);
        assertThat(insightToGenerate.isPresent(), is(Boolean.TRUE));
    }
}

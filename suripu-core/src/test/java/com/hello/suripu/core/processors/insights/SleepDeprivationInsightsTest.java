package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.SleepDeprivationMsgEN;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jarredheinrich on 7/8/16.
 */
public class SleepDeprivationInsightsTest {
    
    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final Account FAKE_ACCOUNT = new Account.Builder().withDOB("1980-01-01").build();
    private final DateTime FAKE_TIMESTAMP = DateTime.now(DateTimeZone.UTC).withHourOfDay(12).minusDays(1);
    private final int OFFSET_MILLIS = -28800000;
    final SleepStats FAKE_SLEEPSTAT_1 = new SleepStats(0,0,0,329,false, 1,0L,0L,5);
    final SleepStats FAKE_SLEEPSTAT_2  = new SleepStats(0,0,0, 420,false, 1,0L,0L,5);

    @Test
    public void testSleepDeprivationInsightGenerated() {

        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = Mockito.mock(SleepStatsDAODynamoDB.class);
        final AccountReadDAO accountReadDAO= Mockito.mock(AccountReadDAO.class);
        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);

        final List<AggregateSleepStats> fakeAggregateSleepStatsList1 = Lists.newArrayList();
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(3), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(2), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(1), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(0), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));

        final List<AggregateSleepStats> fakeAggregateSleepStatsList2 = Lists.newArrayList();
        int i = 20;
        while (i > 6) {
            fakeAggregateSleepStatsList2.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(i), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_2));
            i -= 1;
        }

        final String testQueryStartDate1 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(3));
        final String testQueryEndDate1 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP);
        final String testQueryStartDate2 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(31));
        final String testQueryEndDate2 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(4));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(FAKE_ACCOUNT_ID, testQueryStartDate1,testQueryEndDate1)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList1));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(FAKE_ACCOUNT_ID,testQueryStartDate2, testQueryEndDate2)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList2));
        Mockito.when(accountReadDAO.getById(FAKE_ACCOUNT_ID)).thenReturn(Optional.of(FAKE_ACCOUNT));
        Mockito.when(sleepStatsDAODynamoDB.getTimeZoneOffset(FAKE_ACCOUNT_ID)).thenReturn(Optional.<Integer>absent());

        final int testMeanSleepDebt = 90;

        final Optional<InsightCard> insightGenerated = SleepDeprivation.getInsights(sleepStatsDAODynamoDB, accountReadDAO, FAKE_ACCOUNT_ID, true);
        assertThat(insightGenerated.isPresent(), is(Boolean.TRUE));
        assertThat(insightGenerated.get().message, is(SleepDeprivationMsgEN.getSleepDeprivationMessage(8,testMeanSleepDebt).message) );
    }

    @Test
    public void testSleepDeprivationInsightNotGenerated1() {
        //not 4 consecutive days of sleep deprivation
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = Mockito.mock(SleepStatsDAODynamoDB.class);
        final AccountReadDAO accountReadDAO= Mockito.mock(AccountReadDAO.class);
        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);

        final List<AggregateSleepStats> fakeAggregateSleepStatsList1 = Lists.newArrayList();
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(3), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(2), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_2));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(1), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(0), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));

        final List<AggregateSleepStats> fakeAggregateSleepStatsList2 = Lists.newArrayList();
        int i = 20;
        while (i > 6) {
            fakeAggregateSleepStatsList2.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(i), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_2));
            i -= 1;
        }

        final String testQueryStartDate1 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(3));
        final String testQueryEndDate1 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP);
        final String testQueryStartDate2 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(31));
        final String testQueryEndDate2 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(4));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(FAKE_ACCOUNT_ID, testQueryStartDate1,testQueryEndDate1)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList1));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(FAKE_ACCOUNT_ID,testQueryStartDate2, testQueryEndDate2)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList2));
        Mockito.when(accountReadDAO.getById(FAKE_ACCOUNT_ID)).thenReturn(Optional.of(FAKE_ACCOUNT));
        Mockito.when(sleepStatsDAODynamoDB.getTimeZoneOffset(FAKE_ACCOUNT_ID)).thenReturn(Optional.<Integer>absent());

        final Optional<InsightCard> insightGenerated = SleepDeprivation.getInsights(sleepStatsDAODynamoDB, accountReadDAO, FAKE_ACCOUNT_ID, true);
        assertThat(insightGenerated.isPresent(), is(Boolean.FALSE));
    }


    @Test
    public void testSleepDeprivationInsightNotGenerated2() {
        final Account fakeAccount = new Account.Builder().withDOB("2015-01-01").build();

        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = Mockito.mock(SleepStatsDAODynamoDB.class);
        final AccountReadDAO accountReadDAO= Mockito.mock(AccountReadDAO.class);
        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);

        final List<AggregateSleepStats> fakeAggregateSleepStatsList1 = Lists.newArrayList();
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(3), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(2), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(1), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(0), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_1));

        final List<AggregateSleepStats> fakeAggregateSleepStatsList2 = Lists.newArrayList();
        int i = 20;
        while (i > 6) {
            fakeAggregateSleepStatsList2.add(new AggregateSleepStats(FAKE_ACCOUNT_ID, FAKE_TIMESTAMP.minusDays(i), OFFSET_MILLIS, 0, "1", fakeMotionScore, 0, 0, 0, FAKE_SLEEPSTAT_2));
            i -= 1;
        }

        final String testQueryStartDate1 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(3));
        final String testQueryEndDate1 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP);
        final String testQueryStartDate2 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(31));
        final String testQueryEndDate2 = DateTimeUtil.dateToYmdString(FAKE_TIMESTAMP.minusDays(4));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(FAKE_ACCOUNT_ID, testQueryStartDate1,testQueryEndDate1)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList1));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(FAKE_ACCOUNT_ID,testQueryStartDate2, testQueryEndDate2)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList2));
        Mockito.when(accountReadDAO.getById(FAKE_ACCOUNT_ID)).thenReturn(Optional.of(fakeAccount));
        Mockito.when(sleepStatsDAODynamoDB.getTimeZoneOffset(FAKE_ACCOUNT_ID)).thenReturn(Optional.<Integer>absent());

        final int testMeanSleepDebt = 190;

        final Optional<InsightCard> insightGenerated = SleepDeprivation.getInsights(sleepStatsDAODynamoDB, accountReadDAO, FAKE_ACCOUNT_ID, true);
        assertThat(insightGenerated.isPresent(), is(Boolean.FALSE));
    }

}

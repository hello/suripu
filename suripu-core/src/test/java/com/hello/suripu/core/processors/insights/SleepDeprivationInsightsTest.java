package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.SleepDeprivationMsgEN;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jarredheinrich on 7/8/16.
 */
public class SleepDeprivationInsightsTest {

    @Test
    public void testSleepDeprivationInsightGenerated() {
        final Long testAccountId = 9999L;

        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = Mockito.mock(SleepStatsDAODynamoDB.class);
        final AccountReadDAO accountReadDAO= Mockito.mock(AccountReadDAO.class);

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(12).withMinuteOfHour(0);
        final int offsetMillis = -28800000;

        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);
        final SleepStats fakeSleepStat0 = new SleepStats(0,0,0,290,false, 1,0L,0L,5);
        final SleepStats fakeSleepStat1 = new SleepStats(0,0,0, 480,false, 1,0L,0L,5);

        final List<AggregateSleepStats> fakeAggregateSleepStatsList1 = Lists.newArrayList();
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(3), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(2), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(1), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(0), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));

        final List<AggregateSleepStats> fakeAggregateSleepStatsList2 = Lists.newArrayList();
        int i = 20;
        while (i > 6) {
            fakeAggregateSleepStatsList2.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(i), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat1));
            i -= 1;
        }

        final String testQueryStartDate1 = DateTimeUtil.dateToYmdString(timestamp.minusDays(3));
        final String testQueryEndDate1 = DateTimeUtil.dateToYmdString(timestamp);
        final String testQueryStartDate2 = DateTimeUtil.dateToYmdString(timestamp.minusDays(31));
        final String testQueryEndDate2 = DateTimeUtil.dateToYmdString(timestamp.minusDays(4));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(testAccountId, testQueryStartDate1,testQueryEndDate1)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList1));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(testAccountId,testQueryStartDate2, testQueryEndDate2)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList2));
        Mockito.when(accountReadDAO.getById(testAccountId)).thenReturn(Optional.<Account>absent());

        final int testMeanSleepDebt = 190;
        final int userAge = 30;

        final Optional<InsightCard> insightGenerated = SleepDeprivation.getInsights(sleepStatsDAODynamoDB, accountReadDAO, testAccountId, timestamp);
        assertThat(insightGenerated.isPresent(), is(Boolean.TRUE));
        assertThat(insightGenerated.get().message, is(SleepDeprivationMsgEN.getSleepDeprivationMessage(8,testMeanSleepDebt).message) );
    }

    @Test
    public void testSleepDeprivationInsightNotGenerated() {
        final Long testAccountId = 9999L;

        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = Mockito.mock(SleepStatsDAODynamoDB.class);
        final AccountReadDAO accountReadDAO= Mockito.mock(AccountReadDAO.class);


        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(12).withMinuteOfHour(0);
        final int offsetMillis = -28800000;

        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);
        final SleepStats fakeSleepStat0 = new SleepStats(0,0,0,290,false, 1,0L,0L,5);
        final SleepStats fakeSleepStat1 = new SleepStats(0,0,0, 480,false, 1,0L,0L,5);

        final List<AggregateSleepStats> fakeAggregateSleepStatsList1 = Lists.newArrayList();
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(3), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(2), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(1), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(0), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));

        final List<AggregateSleepStats> fakeAggregateSleepStatsList2 = Lists.newArrayList();
        int i = 20;
        while (i > 6) {
            fakeAggregateSleepStatsList2.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(i), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat1));
            i -= 1;
        }

        final String testQueryStartDate1 = DateTimeUtil.dateToYmdString(timestamp.minusDays(3));
        final String testQueryEndDate1 = DateTimeUtil.dateToYmdString(timestamp);
        final String testQueryStartDate2 = DateTimeUtil.dateToYmdString(timestamp.minusDays(31));
        final String testQueryEndDate2 = DateTimeUtil.dateToYmdString(timestamp.minusDays(4));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(testAccountId, testQueryStartDate1,testQueryEndDate1)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList1));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(testAccountId,testQueryStartDate2, testQueryEndDate2)).thenReturn(ImmutableList.copyOf(fakeAggregateSleepStatsList2));
        Mockito.when(accountReadDAO.getById(testAccountId)).thenReturn(Optional.<Account>absent());

        final int testMeanDuration = 290;
        final int testTotalSleepDur = 1160;
        final int userAge = 30;

        final Optional<InsightCard> insightGenerated = SleepDeprivation.getInsights(sleepStatsDAODynamoDB, accountReadDAO, testAccountId, timestamp);
        assertThat(insightGenerated.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void testNoCardGenerated(){
        final Long testAccountId = 1000L;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(12).withMinuteOfHour(0);
        final int offsetMillis = -28800000;

        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);
        final SleepStats fakeSleepStat0 = new SleepStats(0,0,0,290,false, 1,0L,0L,5);
        final SleepStats fakeSleepStat1 = new SleepStats(0,0,0, 480,false, 1,0L,0L,5);

        final List<AggregateSleepStats> fakeAggregateSleepStatsList1 = Lists.newArrayList();
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(3), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(2), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(1), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat1));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(0), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));

        final List<AggregateSleepStats> fakeAggregateSleepStatsList2 = Lists.newArrayList();
        int i = 20;
        while (i > 6) {
            fakeAggregateSleepStatsList2.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(i), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat1));
            i -= 1;
        }

        final int minSleepDurationMins = 360;
        final int idealSleepDurationhours = 8;

        final Optional<InsightCard> noResults = SleepDeprivation.processSleepDeprivationData(testAccountId, minSleepDurationMins, idealSleepDurationhours, fakeAggregateSleepStatsList1, fakeAggregateSleepStatsList2 );
        assertThat(noResults.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void testCardGenerated(){
        final Long testAccountId = 1000L;

        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(12).withMinuteOfHour(0);
        final int offsetMillis = -28800000;

        final MotionScore fakeMotionScore = Mockito.mock(MotionScore.class);
        final SleepStats fakeSleepStat0 = new SleepStats(0,0,0,290,false, 1,0L,0L,5);
        final SleepStats fakeSleepStat1 = new SleepStats(0,0,0, 480,false, 1,0L,0L,5);

        final List<AggregateSleepStats> fakeAggregateSleepStatsList1 = Lists.newArrayList();
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(3), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(2), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(1), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));
        fakeAggregateSleepStatsList1.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(0), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat0));

        final List<AggregateSleepStats> fakeAggregateSleepStatsList2 = Lists.newArrayList();
        int i = 20;
        while (i > 6) {
            fakeAggregateSleepStatsList2.add(new AggregateSleepStats(testAccountId, timestamp.minusDays(i), offsetMillis, 0, "1", fakeMotionScore, 0, 0, 0, fakeSleepStat1));
            i -= 1;
        }

        final int minSleepDurationMins = 360;
        final int idealSleepDurationhours = 8;

        final Optional<InsightCard> noResults = SleepDeprivation.processSleepDeprivationData(testAccountId, minSleepDurationMins, idealSleepDurationhours, fakeAggregateSleepStatsList1, fakeAggregateSleepStatsList2 );
        assertThat(noResults.isPresent(), is(Boolean.TRUE));
    }
}

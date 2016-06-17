package com.hello.suripu.core.processors.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 5/19/16.
 */
public class SleepAlarmInsightsTest {
    final private long FAKE_ACCOUNT_ID = 999L;
    final private Integer FAKE_AGE_OLD_ADULT = 70;
    final private Integer FAKE_AGE_ADULT = 50;
    final private Integer FAKE_AGE_YOUNG_ADULT = 22;
    final private Integer FAKE_AGE_TEEN = 16;
    final private Integer FAKE_AGE_CHILD = 10;
    final private Integer FAKE_AGE_PRESCHOOL = 4;
    final private Integer FAKE_AGE_NONE = 0;

    @Test
    public void test_CardGenerated_fail() {
        //No wake times
        final List<Integer> wakeTimeList = Lists.newArrayList();

        final Optional<InsightCard> card = SleepAlarm.processSleepAlarm(FAKE_ACCOUNT_ID, wakeTimeList, FAKE_AGE_ADULT, DateTimeFormat.forPattern("h:mm aa"));
        assertThat(card.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_CardGenerated_fail_2() {
        //Not enough wake times
        final List<Integer> wakeTimeList = Lists.newArrayList(1,2);

        final Optional<InsightCard> card = SleepAlarm.processSleepAlarm(FAKE_ACCOUNT_ID, wakeTimeList, FAKE_AGE_ADULT, DateTimeFormat.forPattern("h:mm aa"));
        assertThat(card.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_CardGenerated_fail_3() {
        //Range too large
        final List<Integer> wakeTimeList = Lists.newArrayList(0, 60, 60*10);

        final Optional<InsightCard> card = SleepAlarm.processSleepAlarm(FAKE_ACCOUNT_ID, wakeTimeList, FAKE_AGE_ADULT, DateTimeFormat.forPattern("h:mm aa"));
        assertThat(card.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void test_generateCard() {

        final List<Integer> wakeTimeList = Lists.newArrayList(60*8, 60*9, 60*10);
        final Optional<InsightCard> card = SleepAlarm.processSleepAlarm(FAKE_ACCOUNT_ID, wakeTimeList, FAKE_AGE_ADULT, DateTimeFormat.forPattern("HH:mm"));
//        System.out.print(card.get().message);
        assertThat(card.isPresent(), is(Boolean.TRUE));
    }

    @Test
    public void test_failSafeGuard() {
        //Range greater than 3 hrs
        final List<Integer> wakeTimeList = Lists.newArrayList(0, 60, 60*4);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeguard = SleepAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.FALSE));
    }

    @Test
    public void test_failSafeGuard_2() {
        //Average wake time is afternoon
        final List<Integer> wakeTimeList = Lists.newArrayList(60*12, 60*12, 60*12);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeguard = SleepAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.FALSE));
    }

    @Test
    public void test_failSafeGuard_3() {
        //Average wake time is too early
        final List<Integer> wakeTimeList = Lists.newArrayList(60*1, 60*1, 60*2);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeguard = SleepAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.FALSE));
    }

    @Test
    public void test_passSafeguard() {
        final List<Integer> wakeTimeList = Lists.newArrayList(60*8, 60*9, 60*10);

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Integer wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeguard = SleepAlarm.checkSafeGuards(stats);
        assertThat(passSafeguard, is(Boolean.TRUE));

    }

    @Test
    public void test_getSleepRecommendation() {
        final Integer sleepRecOldAdult = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_OLD_ADULT);
        final Integer sleepRecAdult = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_ADULT);
        final Integer sleepRecYoungAdult = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_YOUNG_ADULT);
        final Integer sleepRecTeen = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_TEEN);
        final Integer sleepRecChild = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_CHILD);
        final Integer sleepRecPreChild = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_PRESCHOOL);
        final Integer sleepRecNone = SleepAlarm.getRecommendedSleepDurationMinutes(FAKE_AGE_NONE);

        final Integer expectedAdult = (8)*60;
        final Integer expectedOlderAdult = (7)*60 + (1)*30;
        final Integer expectedYoungAdult = (8)*60;
        final Integer expectedTeen = (9)*60;
        final Integer expectedChild = (10)*60;
        final Integer expectedPreChild = (11)*60 + (1)*30;

        assertThat(sleepRecOldAdult, is(expectedOlderAdult));
        assertThat(sleepRecAdult, is(expectedAdult));
        assertThat(sleepRecYoungAdult, is(expectedYoungAdult));
        assertThat(sleepRecTeen, is(expectedTeen));
        assertThat(sleepRecChild, is(expectedChild));
        assertThat(sleepRecPreChild, is(expectedPreChild));
        assertThat(sleepRecNone, is(expectedAdult));
    }

    @Test
    public void test_sleepAlarmProcessor() throws IOException {
        final Long FAKE_ACCOUNT_ID = 9999L;

        //Fake sleepStatsDAO
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = Mockito.mock(SleepStatsDAODynamoDB.class);
        final List<AggregateSleepStats> fakeAggSleepStats = Lists.newArrayList(new AggregateSleepStats(FAKE_ACCOUNT_ID, DateTime.now(), 0, 0, "v1", new MotionScore(0, 0, 0f, 0, 0), 0, 0, 0, new SleepStats(0, 0, 0, 0, Boolean.TRUE, 0, 0L, 60*60*1000*9L, 0)),
                new AggregateSleepStats(FAKE_ACCOUNT_ID, DateTime.now(), 0, 0, "v1", new MotionScore(0, 0, 0f, 0, 0), 0, 0, 0, new SleepStats(0, 0, 0, 0, Boolean.TRUE, 0, 0L, 60*60*1000*9L, 0)),
                new AggregateSleepStats(FAKE_ACCOUNT_ID, DateTime.now().minusDays(2), 0, 0, "v1", new MotionScore(0, 0, 0f, 0, 0), 0, 0, 0, new SleepStats(0, 0, 0, 0, Boolean.TRUE, 0, 0L, 60*60*1000*9L, 0)),
                new AggregateSleepStats(FAKE_ACCOUNT_ID, DateTime.now().minusDays(4), 0, 0, "v1", new MotionScore(0, 0, 0f, 0, 0), 0, 0, 0, new SleepStats(0, 0, 0, 0, Boolean.TRUE, 0, 0L, 60*60*1000*10L, 0)));
        final ImmutableList<AggregateSleepStats> immutableAggSleepStats = ImmutableList.copyOf(fakeAggSleepStats);
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(FAKE_ACCOUNT_ID, DateTimeUtil.dateToYmdString(DateTime.now().minusDays(14)), DateTimeUtil.dateToYmdString(DateTime.now()))).thenReturn(immutableAggSleepStats);

        //Fake accountReadDAO
        final AccountReadDAO accountReadDAO = Mockito.mock(AccountDAO.class);
        final ObjectMapper objectMapper = new ObjectMapper();
        final File jsonFile = new File(FileUtils.getResourceFilePath("fixtures/account/valid_account.json"));
        final Account fakeAccount = objectMapper.readValue(jsonFile, Account.class);
        Mockito.when(accountReadDAO.getById(FAKE_ACCOUNT_ID)).thenReturn(Optional.of(fakeAccount));

        final Optional<InsightCard> generatedCard = SleepAlarm.getInsights(sleepStatsDAODynamoDB, accountReadDAO, FAKE_ACCOUNT_ID, DateTimeFormat.forPattern("h:mm aa"));

//        System.out.print(generatedCard.get().message);
        assertThat(generatedCard.isPresent(), is(Boolean.TRUE));
    }

}

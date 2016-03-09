package com.hello.suripu.core.trends.v2;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Created by kingshy on 3/7/16.
 */
public class NewUsersTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewUsersTests.class);

    private SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private AccountDAO accountDAO;
    private TrendsProcessor trendsProcessor;

    @Before
    public void setUp() throws Exception {
        this.sleepStatsDAODynamoDB = mock(SleepStatsDAODynamoDB.class);
        this.timeZoneHistoryDAODynamoDB = mock(TimeZoneHistoryDAODynamoDB.class);
        this.accountDAO = mock(AccountDAO.class);
        this.trendsProcessor = new TrendsProcessor(sleepStatsDAODynamoDB, accountDAO, timeZoneHistoryDAODynamoDB);
    }

    @After
    public void tearDown() {
    }
//    protected TrendsResult getGraphs(final Long accountId, final Integer accountAge, final Optional<DateTime> optionalAccountCreated,
//final DateTime localToday, final TimeScale timescale, final List<TrendsData> rawData) {

    @Test
    public void testNewUsersNoGraphs() {
        final DateTime today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final List<TrendsProcessor.TrendsData> data = Lists.newArrayList();


        int accountAge = 1;
        final TrendsResult trends0 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge - 1)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends0.timeScales.size(), is(0));
        assertThat(trends0.graphs.size(), is(0));

        accountAge = 2;
        final TrendsResult trends1 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge - 1)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends1.timeScales.size(), is(0));
        assertThat(trends1.graphs.size(), is(0));

        accountAge = 3;
        final TrendsResult trends2 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge - 1)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends2.timeScales.size(), is(0));
        assertThat(trends2.graphs.size(), is(0));

        accountAge = 4;
        final TrendsResult trends3 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge - 1)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends3.timeScales.size(), is(0));
        assertThat(trends3.graphs.size(), is(0));

        accountAge = 5;
        final TrendsResult trends4 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge - 1)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends4.timeScales.size(), is(0));
        assertThat(trends4.graphs.size(), is(0));

        accountAge = 6;
        data.add(new TrendsProcessor.TrendsData(today.minusDays(1), 300, 100, 100, 100, 50));
        data.add(new TrendsProcessor.TrendsData(today.minusDays(2), 300, 100, 100, 100, 51));
        final TrendsResult trends5 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge - 1)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends5.timeScales.size(), is(0)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
        assertThat(trends5.graphs.size(), is(0));

        accountAge = 21;
        final TrendsResult trends6 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge - 1)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends5.timeScales.size(), is(0)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
        assertThat(trends5.graphs.size(), is(0));
    }

    @Test
    public void testNewUsersFirstGraphsTimeScaleWeek() {
        // for 5-day old account with 3 valid days of data (first night missing), test today from Sun to Sat
        final DateTime todayStartDate = new DateTime(DateTimeZone.UTC).withDate(2016, 3, 6).withTimeAtStartOfDay();


        for (int accountAge = 4; accountAge <= 10; accountAge++) {
            for (int day = 0; day < DateTimeConstants.DAYS_PER_WEEK + 2; day++) {
                final DateTime today = todayStartDate.plusDays(day);
                final DateTime accountCreated = today.minusDays(accountAge - 1);

                // first night data is missing
                final List<TrendsProcessor.TrendsData> data = Lists.newArrayList(); // data in ascending order
                for (int age = accountAge - 2; age <= 1; age++) {
                    final int extraMins = age * 60;
                    data.add(new TrendsProcessor.TrendsData(today.minusDays(age), 100 + extraMins, 100 + extraMins, 100, 100, 50 + age));
                }

                LOGGER.debug("key=testing-new-users-week account_age={} today={} account_created={} day={}, data_size={}",
                        accountAge, today, accountCreated, day, data.size());

                final TrendsResult trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), today, TimeScale.LAST_WEEK, data);

                if (data.size() < TrendsProcessor.ABSOLUTE_MIN_DATA_SIZE) {
                    assertThat(trends.timeScales.size(), is(0)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
                    assertThat(trends.graphs.size(), is(0));
                    continue;
                }

                assertThat(trends.timeScales.size(), is(1)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
                assertThat(trends.graphs.size(), is(3));

                // check score calendar view
                final Graph scores = trends.graphs.get(0);

                final int yesterdayDOW = today.minusDays(1).getDayOfWeek();
                final int highlightYesterday = (yesterdayDOW == DateTimeConstants.SUNDAY) ? 0 : yesterdayDOW;
                final int accountCreatedDOW = accountCreated.getDayOfWeek();
                final int firstDataIndex = (accountCreatedDOW == DateTimeConstants.SUNDAY) ? 0 : accountCreatedDOW;

                // number of sections
                // final int expectedScoreSections = (yesterdayDOW == DateTimeConstants.SATURDAY) ? 1 : 2;
                final int expectedScoreSections = ((highlightYesterday - firstDataIndex) > 0) ? 1 : 2;
                assertThat(scores.sections.size(), is(expectedScoreSections));

                // check title
                assertThat(scores.sections.get(0).titles.size(), is(DateTimeConstants.DAYS_PER_WEEK));
                assertThat(scores.sections.get(0).titles.get(0).equalsIgnoreCase("sun"), is(true));
                assertThat(scores.sections.get(0).highlightedTitle.isPresent(), is(true));
                assertThat(scores.sections.get(0).highlightedTitle.get(), is(highlightYesterday));

                // check highlighted values
                if (expectedScoreSections == 2) {
                    assertThat(scores.sections.get(0).highlightedValues.size(), is(0));
                    assertThat(scores.sections.get(1).highlightedValues.get(0), is(highlightYesterday));
                } else {
                    assertThat(scores.sections.get(0).highlightedValues.size(), is(1));
                    assertThat(scores.sections.get(0).highlightedValues.get(0), is(highlightYesterday));
                }

                // check values
                // 5th day of account-creation, first day data is missing, value should be -1
                assertThat(scores.sections.get(0).values.get(firstDataIndex), is(GraphSection.MISSING_VALUE));

                // check last value
                // check highlighted value, should be yesterday, last non-null value in the last section
                final float lastScoreValue = (float) data.get(data.size() - 1).getScore();
                assertThat(scores.sections.get(expectedScoreSections - 1).values.get(highlightYesterday), is(lastScoreValue));

                // check value for today should be null
                if (expectedScoreSections > 1) {
                    assertThat(scores.sections.get(expectedScoreSections - 1).values.get(highlightYesterday + 1) == null, is(true));
                }
            }
        }

    }

}
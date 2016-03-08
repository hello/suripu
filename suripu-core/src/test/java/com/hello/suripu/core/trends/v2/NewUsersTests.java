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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Created by kingshy on 3/7/16.
 */
public class NewUsersTests {
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

        int accountAge = 0;
        final TrendsResult trends0 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends0.timeScales.size(), is(0));
        assertThat(trends0.graphs.size(), is(0));

        accountAge = 1;
        final TrendsResult trends1 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends1.timeScales.size(), is(0));
        assertThat(trends1.graphs.size(), is(0));

        accountAge = 2;
        final TrendsResult trends2 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends2.timeScales.size(), is(0));
        assertThat(trends2.graphs.size(), is(0));

        accountAge = 3;
        final TrendsResult trends3 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends3.timeScales.size(), is(0));
        assertThat(trends3.graphs.size(), is(0));

        accountAge = 4;
        final TrendsResult trends4 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends4.timeScales.size(), is(0));
        assertThat(trends4.graphs.size(), is(0));

        accountAge = 4;
        data.add(new TrendsProcessor.TrendsData(today.minusDays(1), 300, 100, 100, 100, 50));
        data.add(new TrendsProcessor.TrendsData(today.minusDays(2), 300, 100, 100, 100, 51));
        final TrendsResult trends5 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends5.timeScales.size(), is(0)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
        assertThat(trends5.graphs.size(), is(0));

        accountAge = 20;
        final TrendsResult trends6 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends5.timeScales.size(), is(0)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
        assertThat(trends5.graphs.size(), is(0));
    }

    @Test
    public void testNewUsersFirstGraphs() {
        final DateTime today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();

        final List<TrendsProcessor.TrendsData> data = Lists.newArrayList(); // data in ascending order
        data.add(new TrendsProcessor.TrendsData(today.minusDays(3), 420, 200, 100, 100, 53));
        data.add(new TrendsProcessor.TrendsData(today.minusDays(2), 360, 160, 100, 100, 51));
        data.add(new TrendsProcessor.TrendsData(today.minusDays(1), 300, 100, 100, 100, 50));


        int accountAge = 4;
        final DateTime accountCreated = today.minusDays(accountAge);
        final TrendsResult trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), today, TimeScale.LAST_WEEK, data);
        assertThat(trends.timeScales.size(), is(1)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
        assertThat(trends.graphs.size(), is(3));

        // check score calendar view
        final Graph scores = trends.graphs.get(0);

        final int yesterday = today.minusDays(1).getDayOfWeek();
        final int highlightedTitle = (yesterday == DateTimeConstants.SUNDAY) ? 0 : yesterday - 1;
        final int expectedScoreSections = (yesterday != DateTimeConstants.SATURDAY) ? 3 : 2;
        assertThat(scores.sections.size(), is(expectedScoreSections));

        assertThat(scores.sections.get(0).titles.size(), is(DateTimeConstants.DAYS_PER_WEEK));
        assertThat(scores.sections.get(0).titles.get(0).equalsIgnoreCase("sun"), is(true));
        assertThat(scores.sections.get(0).highlightedValues.size(), is(0));
        assertThat(scores.sections.get(0).highlightedTitle.isPresent(), is(true));
        assertThat(scores.sections.get(0).highlightedTitle.get(), is(highlightedTitle));




    }
}

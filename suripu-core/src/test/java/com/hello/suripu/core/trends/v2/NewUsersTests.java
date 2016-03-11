package com.hello.suripu.core.trends.v2;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
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
        assertThat(trends5.timeScales.size(), is(1)); // TODO New method, should be 0. need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
        assertThat(trends5.graphs.size(), is(0));

        accountAge = 21;
        final TrendsResult trends6 = trendsProcessor.getGraphs(10L, accountAge, Optional.of(today.minusDays(accountAge - 1)), today, TimeScale.LAST_WEEK, data);
        assertThat(trends6.timeScales.size(), is(2)); // TODO New method, should be 0. need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
        assertThat(trends6.graphs.size(), is(0));
    }

    @Test
    public void testNewUsersScoreWeek() {
        // for 5-day old account with 3 valid days of data (first night missing), test today from Sun to Sat
        final DateTime todayStartDate = new DateTime(DateTimeZone.UTC).withDate(2016, 2, 28).withTimeAtStartOfDay();

        for (int accountAge = 4; accountAge <= 10; accountAge++) {
            for (int day = 0; day < DateTimeConstants.DAYS_PER_WEEK * 2; day++) {
                final DateTime today = todayStartDate.plusDays(day);
                final DateTime accountCreated = today.minusDays(accountAge - 1);

                // first night data is missing
                final List<TrendsProcessor.TrendsData> data = Lists.newArrayList(); // data in ascending order
                final int startAge = Math.min(accountAge - 2, 6);
                for (int age = startAge; age >= 1; age--) {
                    final int durationMinutes = 100 + (age * 60);
                    final DateTime dataDate = today.minusDays(age);
                    data.add(new TrendsProcessor.TrendsData(dataDate, durationMinutes, 100 + (age * 60), 100, 100, 50 + age));
                    if (data.size() >= 6) {
                        break;
                    }
                }

                LOGGER.debug("key=testing-new-users-week account_age={} today={} account_created={} day={}, data_size={}",
                        accountAge, today, accountCreated, day, data.size());

                final TrendsResult trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), today, TimeScale.LAST_WEEK, data);

                if (data.size() < TrendsProcessor.ABSOLUTE_MIN_DATA_SIZE) {
                    // assertThat(trends.timeScales.size(), is(0)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
                    if (accountAge <= TimeScale.LAST_WEEK.getVisibleAfterDays()) {
                        assertThat(trends.timeScales.size(), is(0));
                    } else if (accountAge <= TimeScale.LAST_MONTH.getVisibleAfterDays()) {
                        assertThat(trends.timeScales.size(), is(1));
                    } else if (accountAge <= TimeScale.LAST_3_MONTHS.getVisibleAfterDays()) {
                        assertThat(trends.timeScales.size(), is(2));
                    } else {
                        assertThat(trends.timeScales.size(), is(3));
                    }
                    assertThat(trends.graphs.size(), is(0));
                    continue;
                }

                final int expectedTimeScalesSize;
                if (accountAge > TimeScale.LAST_MONTH.getVisibleAfterDays()) {
                    expectedTimeScalesSize = 2;
                } else {
                    expectedTimeScalesSize = 1;
                }

                assertThat(trends.timeScales.size(), is(expectedTimeScalesSize)); // need min ABSOLUTE_MIN_DATA_SIZE to get TimeScale
                assertThat(trends.graphs.size(), is(3));

                // check score calendar view
                final Graph scores = trends.graphs.get(0);

                final int yesterdayDOW = today.minusDays(1).getDayOfWeek();
                final int highlightYesterday = (yesterdayDOW == DateTimeConstants.SUNDAY) ? 0 : yesterdayDOW;

                final int accountCreatedDOW = accountCreated.getDayOfWeek();
                final int firstDataIndex;
                if (accountAge <= DateTimeConstants.DAYS_PER_WEEK) {
                    // first data point should be accountCreated date
                    firstDataIndex = (accountCreatedDOW == DateTimeConstants.SUNDAY) ? 0 : accountCreatedDOW;
                } else {
                    final int aWeekAgoDOW = today.minusDays(DateTimeConstants.DAYS_PER_WEEK).getDayOfWeek();
                    firstDataIndex = (aWeekAgoDOW == DateTimeConstants.SUNDAY) ? 0 : aWeekAgoDOW;
                }

                // number of sections
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

    @Test
    public void testNewUsersDurationWeek() {

        final DateTime todayStartDate = new DateTime(DateTimeZone.UTC).withDate(2016, 2, 28).withTimeAtStartOfDay();

        for (int accountAge = 3; accountAge <= 10; accountAge++) {
            for (int day = 0; day < DateTimeConstants.DAYS_PER_WEEK * 2; day++) {
                final DateTime today = todayStartDate.plusDays(day);
                final DateTime accountCreated = today.minusDays(accountAge - 1);

                // first night data is missing
                final List<TrendsProcessor.TrendsData> data = Lists.newArrayList(); // data in ascending order
                final int startAge = Math.min(accountAge - 2, 6);
                boolean hasWeekDays = false;
                boolean hasWeekEnd = false;
                for (int age = startAge; age >= 1; age--) {
                    final DateTime dataDate = today.minusDays(age);

                    final int durationMinutes = 100 + (age * 60);

                    // for annotation checks
                    if (dataDate.getDayOfWeek() < DateTimeConstants.SATURDAY) {
                        hasWeekDays = true;
                    } else if (dataDate.getDayOfWeek() >= DateTimeConstants.SATURDAY) {
                        hasWeekEnd = true;
                    }

                    data.add(new TrendsProcessor.TrendsData(dataDate, durationMinutes, 100 + (age * 60), 100, 100, 50 + age));

                    if (data.size() >= 6) {
                        break;
                    }
                }

                LOGGER.debug("key=testing-new-users-week account_age={} today={} account_created={} day={}, data_size={}",
                        accountAge, today, accountCreated, day, data.size());

                final boolean hasAnnotation = (accountAge >= Annotation.ANNOTATION_ENABLED_THRESHOLD);
                final Optional<Graph> optionalGraph = this.trendsProcessor.getDaysGraph(data,
                        TimeScale.LAST_WEEK,
                        GraphType.BAR,
                        DataType.HOURS,
                        English.GRAPH_TITLE_SLEEP_DURATION,
                        today, hasAnnotation, Optional.of(accountCreated));


                // Check Duration Bar graphs
                final Graph duration = optionalGraph.get();
                assertThat(duration.title.equalsIgnoreCase("Sleep Duration"), is(true));

                // annotation present if weekdays and weekends are present
                final int annotationSize = (hasWeekDays && hasWeekEnd && accountAge >= Annotation.ANNOTATION_ENABLED_THRESHOLD) ? 3 : 0;
                assertThat(duration.annotations.size(), is(annotationSize));

                // Highlighted title for WEEK bar graph is always the last value of the last section
                final int barNumSections = duration.sections.size();
                final int barHighlightTitle = DateTimeConstants.DAYS_PER_WEEK - 1;
                assertThat(duration.sections.get(barNumSections - 1).highlightedTitle.get(), is(barHighlightTitle));

                // check number of sections, should always be 1 for week graph
                assertThat(duration.sections.size(), is(1));

                final GraphSection section = duration.sections.get(0);

                // check last title, should be Yesterday
                final int yesterdayDOW = today.minusDays(1).getDayOfWeek();
                final String yesterdayString = (yesterdayDOW == DateTimeConstants.SUNDAY) ? "sun" : English.DAY_OF_WEEK_NAMES.get(yesterdayDOW);
                assertThat(section.titles.get(6).equalsIgnoreCase(yesterdayString), is(true));

                // check first and last values
                assertThat(section.values.size(), is(7));
                assertThat(section.values.get(0), is(GraphSection.MISSING_VALUE));
                assertThat(section.values.get(6), is((float) data.get(data.size()-1).getDuration()/60.0f));
            }
        }
    }


    @Test
    public void spencerDev570Case() {
        // only two nights of data in the past week, should not have min/max highlights
        final DateTime accountCreated = new DateTime(DateTimeZone.UTC).withDate(2016, 3, 9).withTimeAtStartOfDay(); // Wed

        final List<TrendsProcessor.TrendsData> data = Lists.newArrayList(); // data in ascending order

        // today = day 2, 03/10
        data.add(new TrendsProcessor.TrendsData(accountCreated.plusDays(0), 500, 300, 100, 100, 50)); // day 1 03/09 night
        DateTime localToday = accountCreated.plusDays(1);
        int accountAge = Days.daysBetween(accountCreated, localToday).getDays() + 1;
        TrendsResult trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), localToday, TimeScale.LAST_WEEK, data);

        assertThat(trends.timeScales.size(), is(0));
        assertThat(trends.graphs.size(), is(0));

        // today = day 3, 03/11
        localToday = accountCreated.plusDays(2);
        data.add(new TrendsProcessor.TrendsData(accountCreated.plusDays(1), 501, 300, 100, 100, 51)); // day 2 03/10 night
        accountAge = Days.daysBetween(accountCreated, localToday).getDays() + 1;
        trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), localToday, TimeScale.LAST_WEEK, data);
        assertThat(trends.timeScales.size(), is(0));
        assertThat(trends.graphs.size(), is(0));

        // today = day 4, 03/12, no data for 03/11
        localToday = accountCreated.plusDays(3);
        accountAge = Days.daysBetween(accountCreated, localToday).getDays() + 1;
        trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), localToday, TimeScale.LAST_WEEK, data);
        assertThat(trends.timeScales.size(), is(1));
        assertThat(trends.graphs.size(), is(0));

        // today = day 5, 03/13, no data for 03/12
        localToday = accountCreated.plusDays(4);
        accountAge = Days.daysBetween(accountCreated, localToday).getDays() + 1;
        trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), localToday, TimeScale.LAST_WEEK, data);
        assertThat(trends.timeScales.size(), is(1));
        assertThat(trends.graphs.size(), is(0));

        // today = day 6, 03/14, data for 03/13 Sun
        localToday = accountCreated.plusDays(5);
        data.add(new TrendsProcessor.TrendsData(accountCreated.plusDays(4), 504, 300, 100, 100, 54)); // day 2 03/10 night
        accountAge = Days.daysBetween(accountCreated, localToday).getDays() + 1;
        trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), localToday, TimeScale.LAST_WEEK, data);
        assertThat(trends.timeScales.size(), is(1));
        assertThat(trends.graphs.size(), is(3));

        // today = day 7, 03/15, data for 03/14 mon night
        localToday = accountCreated.plusDays(6);
        data.add(new TrendsProcessor.TrendsData(accountCreated.plusDays(5), 505, 300, 100, 100, 55)); // day 2 03/10 night
        accountAge = Days.daysBetween(accountCreated, localToday).getDays() + 1;
        trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), localToday, TimeScale.LAST_WEEK, data);
        assertThat(trends.timeScales.size(), is(1));
        assertThat(trends.graphs.size(), is(3));

        // today = day 8, 03/16, data for 03/15 night
        localToday = accountCreated.plusDays(7);
        data.add(new TrendsProcessor.TrendsData(accountCreated.plusDays(6), 505, 300, 100, 100, 55)); // day 2 03/10 night
        accountAge = Days.daysBetween(accountCreated, localToday).getDays() + 1;
        trends = trendsProcessor.getGraphs(10L, accountAge, Optional.of(accountCreated), localToday, TimeScale.LAST_WEEK, data);
        assertThat(trends.timeScales.size(), is(2));
        assertThat(trends.graphs.size(), is(3));

    }
}
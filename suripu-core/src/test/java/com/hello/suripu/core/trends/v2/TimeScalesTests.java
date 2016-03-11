package com.hello.suripu.core.trends.v2;

import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by ksg on 3/4/16
 */
public class TimeScalesTests {

    private SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private AccountDAO accountDAO;
    private TrendsProcessor trendsProcessor;

    @Before
    public void setUp() throws Exception {
//        final BasicAWSCredentials awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
//        final ClientConfiguration clientConfiguration = new ClientConfiguration();
//        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentials, clientConfiguration);
//        dynamoDBClient.setEndpoint("http://localhost:7777");

        this.sleepStatsDAODynamoDB = mock(SleepStatsDAODynamoDB.class);
        this.timeZoneHistoryDAODynamoDB = mock(TimeZoneHistoryDAODynamoDB.class);
        this.accountDAO = mock(AccountDAO.class);

        this.trendsProcessor = new TrendsProcessor(sleepStatsDAODynamoDB, accountDAO, timeZoneHistoryDAODynamoDB);
    }

    @After
    public void tearDown() {
    }

    private int getNumTimeScales(final int accountAge) {
        if (accountAge <= TimeScale.LAST_WEEK.getVisibleAfterDays()) {
            return 0;
        } else if (accountAge <= TimeScale.LAST_MONTH.getVisibleAfterDays()) {
            return 1;
        } else if (accountAge <= TimeScale.LAST_3_MONTHS.getVisibleAfterDays()) {
            return 2;
        } else {
            return 3;
        }

    }
    @Test
    public void accountLessThan3DaysOld() {
        // no timescales returned
        final DateTime today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final List<TrendsProcessor.TrendsData> dataList = Lists.newArrayList();

        for (int accountAge = 0; accountAge < 31; accountAge++) {
            List<TimeScale> availableTimeScales;
            availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
            // availableTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);

            assertThat(availableTimeScales.size(), is(getNumTimeScales(accountAge)));
        }

        // one data-point ever, no timescales
        int num = 1;
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(num), 600, 100, 400, 100, 55));
        for (int accountAge = num; accountAge < 31; accountAge++) {
            List<TimeScale> availableTimeScales;
            availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
            // availableTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);

            assertThat(availableTimeScales.size(), is(getNumTimeScales(accountAge)));
        }

        // two data-points ever, no timescales
        num = 2;
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(num), 500, 100, 300, 100, 45));
        for (int accountAge = num; accountAge < 10; accountAge++) {
            List<TimeScale> availableTimeScales;
            availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
            // availableTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);
            assertThat(availableTimeScales.size(), is(getNumTimeScales(accountAge)));
        }

        // three data-points, should see at least one timescale for accounts older than 3 days
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(3), 400, 100, 200, 100, 35));

        List<TimeScale> availableTimeScales;

        int accountAge = 3; // timescale = []
        availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
        // availableTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);
        assertThat(availableTimeScales.size(), is(0));

        accountAge = 4; // timescale = [WEEK]
        availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
        // availableTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);
        assertThat(availableTimeScales.size(), is(1));

        accountAge = 7; // timescale = [WEEK]
        availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
        // availableTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);
        assertThat(availableTimeScales.size(), is(1));
        assertThat(availableTimeScales.get(0).equals(TimeScale.LAST_WEEK), is(true));

        accountAge = 8; // timescale = [WEEK, MONTH]
        availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
        // availableTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);
        assertThat(availableTimeScales.size(), is(2));
        assertThat(availableTimeScales.get(1).equals(TimeScale.LAST_MONTH), is(true));
    }

    @Test
    public void account3DaysOrOlder() {
        final DateTime accountCreated = new DateTime(DateTimeZone.UTC).withYear(2016).withMonthOfYear(3).withDayOfMonth(1).withTimeAtStartOfDay();

        final DateTime today = new DateTime(DateTimeZone.UTC).withYear(2016).withMonthOfYear(3).withDayOfMonth(7).withTimeAtStartOfDay();
        final int accountAge = Days.daysBetween(accountCreated, today).getDays() + 1;
        assertThat(accountAge, is(7));

        final List<TrendsProcessor.TrendsData> dataList = Lists.newArrayList();
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(3), 400, 100, 200, 100, 35));
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(2), 500, 200, 200, 100, 35));

        // account-age = 7, data-size = 2, timescales = []
        final List<TimeScale> availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
        // final List<TimeScale> availableTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);

        assertThat(availableTimeScales.size(), is(1)); // new method should be 0

        // account-age = 7, data-size = 3, timescales = [WEEK]
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(1), 500, 200, 200, 100, 35));
        final List<TimeScale> weekTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge);
        //final List<TimeScale> weekTimeScales = this.trendsProcessor.computeAvailableTimeScalesNew(accountAge, today, dataList);
        assertThat(weekTimeScales.size(), is(1));
        assertThat(weekTimeScales.get(0).equals(TimeScale.LAST_WEEK), is(true));


        // account-age = 8, data-size = 3, timescales = [WEEK, MONTH]
        final DateTime today2 = new DateTime(DateTimeZone.UTC).withYear(2016).withMonthOfYear(3).withDayOfMonth(8).withTimeAtStartOfDay();
        final int accountAge2 = Days.daysBetween(accountCreated, today2).getDays() + 1;
        assertThat(accountAge2, is(8));

        final List<TimeScale> monthTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge2);
        // final List<TimeScale> monthTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge2, today2, dataList);

        assertThat(monthTimeScales.size(), is(2));
        assertThat(monthTimeScales.get(0).equals(TimeScale.LAST_WEEK), is(true));
        assertThat(monthTimeScales.get(1).equals(TimeScale.LAST_MONTH), is(true));


        // account-age = 30, data-size = 3 (none in past week), timescales = [WEEK, MONTH]
        final DateTime today3 = new DateTime(DateTimeZone.UTC).withYear(2016).withMonthOfYear(3).withDayOfMonth(30).withTimeAtStartOfDay();
        final int accountAge3 = Days.daysBetween(accountCreated, today3).getDays() + 1;
        assertThat(accountAge3, is(30));

        final List<TimeScale> monthTimeScales3 = this.trendsProcessor.computeAvailableTimeScales(accountAge3);
        // final List<TimeScale> monthTimeScales3 = this.trendsProcessor.computeAvailableTimeScales(accountAge3, today3, dataList);

        assertThat(monthTimeScales3.size(), is(2));
        assertThat(monthTimeScales3.get(0).equals(TimeScale.LAST_WEEK), is(true));
        assertThat(monthTimeScales3.get(1).equals(TimeScale.LAST_MONTH), is(true));


        // account-age = 31, data-size = 3 (none in past week), timescale = [WEEK, MONTH, QUARTER]
        final DateTime today4 = new DateTime(DateTimeZone.UTC).withYear(2016).withMonthOfYear(3).withDayOfMonth(31).withTimeAtStartOfDay();
        final int accountAge4 = Days.daysBetween(accountCreated, today4).getDays() + 1;
        assertThat(accountAge4, is(31));

        final List<TimeScale> timeScales4 = this.trendsProcessor.computeAvailableTimeScales(accountAge4);
        // final List<TimeScale> timeScales4 = this.trendsProcessor.computeAvailableTimeScales(accountAge4, today4, dataList);
        assertThat(timeScales4.size(), is(3));
        assertThat(timeScales4.get(0).equals(TimeScale.LAST_WEEK), is(true));
        assertThat(timeScales4.get(1).equals(TimeScale.LAST_MONTH), is(true));
        assertThat(timeScales4.get(2).equals(TimeScale.LAST_3_MONTHS), is(true));

        // account-age = 101, data-size = 3 (none in past month), timescale = [WEEK, MONTH, QUARTER]
        final DateTime today5 = accountCreated.plusDays(100); // 2016-06-09
        final int accountAge5 = Days.daysBetween(accountCreated, today5).getDays() + 1;
        assertThat(accountAge5, is(101));

        final List<TimeScale> timeScales5 = this.trendsProcessor.computeAvailableTimeScales(accountAge5);
        // final List<TimeScale> timeScales5 = this.trendsProcessor.computeAvailableTimeScales(accountAge5, today5, dataList);

        assertThat(timeScales5.size(), is(3));
        assertThat(timeScales5.get(0).equals(TimeScale.LAST_WEEK), is(true));
        assertThat(timeScales5.get(1).equals(TimeScale.LAST_MONTH), is(true));
        assertThat(timeScales5.get(2).equals(TimeScale.LAST_3_MONTHS), is(true));

    }
}
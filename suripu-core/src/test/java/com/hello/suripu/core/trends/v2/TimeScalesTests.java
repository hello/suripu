package com.hello.suripu.core.trends.v2;

import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

    @Test
    public void accountLessThan3DaysOld() {
        // no timescales returned
        final DateTime today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final List<TrendsProcessor.TrendsData> dataList = Lists.newArrayList();

        for (int accountAge = 0; accountAge < 10; accountAge++) {
            List<TimeScale> availableTimeScales;
            availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge, today, dataList);
            assertThat(availableTimeScales.size(), is(0));
        }

        // one data-point
        int num = 1;
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(num), 600, 100, 400, 100, 55));
        for (int accountAge = num; accountAge < 10; accountAge++) {
            List<TimeScale> availableTimeScales;
            availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge, today, dataList);
            assertThat(availableTimeScales.size(), is(0));
        }

        // two data-points
        num = 2;
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(num), 500, 100, 300, 100, 45));
        for (int accountAge = num; accountAge < 10; accountAge++) {
            List<TimeScale> availableTimeScales;
            availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge, today, dataList);
            assertThat(availableTimeScales.size(), is(0));
        }

        // three data-points
        num = 3;
        dataList.add(new TrendsProcessor.TrendsData(today.minusDays(num), 400, 100, 200, 100, 35));
        int accountAge = 3;

        List<TimeScale> availableTimeScales;
        availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge, today, dataList);
        assertThat(availableTimeScales.size(), is(0));

        accountAge = 7;
        availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge, today, dataList);
        assertThat(availableTimeScales.size(), is(1));

        // should see month
        accountAge = 8;
        availableTimeScales = this.trendsProcessor.computeAvailableTimeScales(accountAge, today, dataList);
        assertThat(availableTimeScales.size(), is(2));
        assertThat(availableTimeScales.get(1).equals(TimeScale.LAST_MONTH), is(true));

    }
}
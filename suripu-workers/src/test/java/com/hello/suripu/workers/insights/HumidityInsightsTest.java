package com.hello.suripu.workers.insights;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.processors.insights.BedLightDuration;
import com.hello.suripu.core.processors.insights.Humidity;
import com.hello.suripu.core.util.DateTimeUtil;
import org.h2.api.Aggregate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 9/18/15.
 */
public class HumidityInsightsTest {

    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final Long FAKE_DEVICE_ID = 9998L;
    private final DateTime STANDARD_TIMESTAMP = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();

    private final DateTime FAKE_TIMESTAMP = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
    private final int FAKE_OFFSET_MILLIS = -28800000;

    private static final Integer PRE_BED_BEGIN_HOUR_LOCAL = 21; // 9pm
    private static final Integer PRE_BED_END_HOUR_LOCAL = 1; // 2am

    /*
    @Test
    public void test_noCard() {
        final DeviceDataDAO deviceDataDAO = Mockito.mock(DeviceDataDAO.class);
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = Mockito.mock(SleepStatsDAODynamoDB.class);

        final int FAKE_OFFSET_MILLIS = -28800000;
        final DateTime queryEndTime = DateTime.now(DateTimeZone.forFAKE_OFFSET_MILLIS(FAKE_OFFSET_MILLIS)).withHourOfDay(PRE_BED_BEGIN_HOUR_LOCAL);
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.PAST_WEEK);
//        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);


        final List<DeviceData> data = Lists.newArrayList();
        Mockito.when(deviceDataDAO.getBetweenHourDateByTS(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, queryStartTime, queryEndTime, PRE_BED_BEGIN_HOUR_LOCAL, PRE_BED_END_HOUR_LOCAL))
                .thenReturn(ImmutableList.copyOf(data));

        final AggregateSleepStats mockSleepStats = Mockito.mock(AggregateSleepStats.class);
        Mockito.when(mockSleepStats.FAKE_OFFSET_MILLIS).thenReturn(FAKE_OFFSET_MILLIS);
        final List<AggregateSleepStats> mockSleepStatsList = Lists.newArrayList(mockSleepStats);

        final String  queryEndDate = DateTimeUtil.dateToYmdString(queryEndTime);
        final String  queryStartDate = DateTimeUtil.dateToYmdString(queryEndTime.minusDays(1));
        Mockito.when(sleepStatsDAODynamoDB.getBatchStats(FAKE_ACCOUNT_ID, queryStartDate, queryEndDate))
                .thenReturn(ImmutableList.copyOf(mockSleepStatsList));


        final Optional<InsightCard> noResult = Humidity.getInsights(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, deviceDataDAO, sleepStatsDAODynamoDB);

        assertThat(noResult.isPresent(), is(Boolean.FALSE));
    }
    */

    @Test
    public void test_processData() {
//        data.add(new DeviceData(0L, 0L, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, timestamp, FAKE_OFFSET_MILLIS, 1, 1, 1, 0, 0, 0));

    }

    @Test
    public void test_getMedianHumidity() {
        
        final int humidity = 50;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, humidity, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, FAKE_OFFSET_MILLIS, 0, 0, 0, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, humidity + 5, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, FAKE_OFFSET_MILLIS, 0, 0, 0, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, humidity - 1, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, FAKE_OFFSET_MILLIS, 0, 0, 0, 0, 0, 0));


    }

    @Test
    public void test_getDeviceData() {

    }

    @Test
    public void test_getTimeZoneOffsetOptional() {

    }
}

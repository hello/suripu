package com.hello.suripu.core.insights.models;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.insights.models.SoundDisturbance;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.models.text.SoundDisturbanceMsgEN;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 9/25/15.
 */
public class SoundDisturbanceInsightsTest {

    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final Long FAKE_DEVICE_ID = 9998L;

    private final DateTime FAKE_TIMESTAMP = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
    private final int SF_OFFSET_MILLIS = -28800000;

    @Test
    public void test_processData_none() {
        final Optional<InsightCard> result = SoundDisturbance.processData(FAKE_ACCOUNT_ID, 100);
        assertThat(result.isPresent(), is(Boolean.FALSE));
    }
    
    @Test
    public void test_processData_high() {
        final Optional<InsightCard> result = SoundDisturbance.processData(FAKE_ACCOUNT_ID, 1500);
        final String title = result.get().title;
        final String expectedTitle = SoundDisturbanceMsgEN.getHighSumDisturbance().title;

        assertThat(title, is(expectedTitle));
    }

    @Test
    public void test_processData_veryHigh() {
        final Optional<InsightCard> result = SoundDisturbance.processData(FAKE_ACCOUNT_ID, 2500);
        final String title = result.get().title;
        final String expectedTitle = SoundDisturbanceMsgEN.getVeryHighSumDisturbance().title;

        assertThat(title, is(expectedTitle));
    }
    
    @Test
    public void test_getSumDisturbance() {

        final int numDisturbance = 2;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, SF_OFFSET_MILLIS, 0, 0, 0, numDisturbance, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, SF_OFFSET_MILLIS, 0, 0, 0, numDisturbance, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, SF_OFFSET_MILLIS, 0, 0, 0, numDisturbance, 0, 0, 0));

        final Integer result = SoundDisturbance.getSumDisturbance(data);
        final Integer expectedResult = 6;

        assertThat(result, is(expectedResult));
    }

    @Test
    public void test_getDeviceDataHelper_case1() {

        final DateTime queryDate = DateTime.parse("2015-01-01").withHourOfDay(10);

        final DateTime queryEndTime = SoundDisturbance.getDeviceDataQueryDate(queryDate);
        final DateTime expectedResult = queryDate.minusDays(1).withHourOfDay(12);

        assertThat(queryEndTime, is(expectedResult));
    }


    @Test
    public void test_getDeviceDataHelper_case2() {

        final DateTime queryDate = DateTime.parse("2015-01-01").withHourOfDay(20);

        final DateTime queryEndTime = SoundDisturbance.getDeviceDataQueryDate(queryDate);
        final DateTime expectedResult = queryDate.withHourOfDay(12);

        assertThat(queryEndTime, is(expectedResult));
    }


}

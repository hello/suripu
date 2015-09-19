package com.hello.suripu.workers.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.HumidityMsgEN;
import com.hello.suripu.core.processors.insights.Humidity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 9/18/15.
 */
public class HumidityInsightsTest {

    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final Long FAKE_DEVICE_ID = 9998L;

    private final DateTime FAKE_TIMESTAMP = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
    private final int FAKE_OFFSET_MILLIS = -28800000;

    @Test
    public void test_getMedianHumidity() {
        
        final int humidity = 50;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, humidity, 0, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, FAKE_OFFSET_MILLIS, 0, 0, 0, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, humidity + 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, FAKE_OFFSET_MILLIS, 0, 0, 0, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, humidity - 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, FAKE_OFFSET_MILLIS, 0, 0, 0, 0, 0, 0));


        final Integer result = Humidity.getMedianHumidity(data);
        final Integer expectedResult = humidity;

        assertThat(result, is(expectedResult));

    }

    @Test
    public void test_getMedianHumidity_2() {
        //Test that casting median humidity to Integer works with even number of data points too

        final int humidity = 50;

        final List<DeviceData> data = Lists.newArrayList();
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, humidity, 0, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, FAKE_OFFSET_MILLIS, 0, 0, 0, 0, 0, 0));
        data.add(new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, humidity + 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, FAKE_TIMESTAMP, FAKE_OFFSET_MILLIS, 0, 0, 0, 0, 0, 0));


        final Integer result = Humidity.getMedianHumidity(data);
        final Integer expectedResult = humidity;

        assertThat(result, is(expectedResult));
    }

    @Test
    public void test_processData_veryLowHumidity() {
        final Optional<InsightCard> result = Humidity.processData(FAKE_ACCOUNT_ID, 0);
        final String title = result.get().title;
        final String expectedTitle = HumidityMsgEN.getVeryLowHumidity().title;

        assertThat(title, is(expectedTitle));
    }

    @Test
    public void test_processData_lowHumidity() {
        final Optional<InsightCard> result = Humidity.processData(FAKE_ACCOUNT_ID, 25);
        final String title = result.get().title;
        final String expectedTitle = HumidityMsgEN.getLowHumidity().title;

        assertThat(title, is(expectedTitle));
    }

    @Test
    public void test_processData_idealHumidity() {
        final Optional<InsightCard> result = Humidity.processData(FAKE_ACCOUNT_ID, 50);
        final String title = result.get().title;
        final String expectedTitle = HumidityMsgEN.getIdealHumidity().title;

        assertThat(title, is(expectedTitle));
    }

    @Test
    public void test_processData_highHumidity() {
        final Optional<InsightCard> result = Humidity.processData(FAKE_ACCOUNT_ID, 65);
        final String title = result.get().title;
        final String expectedTitle = HumidityMsgEN.getHighHumidity().title;

        assertThat(title, is(expectedTitle));
    }

    @Test
    public void test_processData_veryHighHumidity() {
        final Optional<InsightCard> result = Humidity.processData(FAKE_ACCOUNT_ID, 100);
        final String title = result.get().title;
        final String expectedTitle = HumidityMsgEN.getVeryHighHumidity().title;

        assertThat(title, is(expectedTitle));
    }
}

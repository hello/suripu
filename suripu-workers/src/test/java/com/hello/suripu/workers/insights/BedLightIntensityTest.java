package com.hello.suripu.workers.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.BedLightIntensityMsgEN;
import com.hello.suripu.core.processors.insights.BedLightIntensity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Created by jyfan on 8/27/15.
 */
public class BedLightIntensityTest {

    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final Long FAKE_DEVICE_ID = 9998L;

    private final DateTime TIMESTAMP_NOW = DateTime.now(DateTimeZone.UTC).withHourOfDay(19).withMinuteOfHour(0);
    private final int OFFSET_MILLIS_SF = -28800000;

    @Test
    public void testIntegrateLight() {

        final List<DeviceData> data = Lists.newArrayList();
        data.add(createNewDeviceData(2, TIMESTAMP_NOW));
        data.add(createNewDeviceData(2, TIMESTAMP_NOW));
        data.add(createNewDeviceData(0, TIMESTAMP_NOW));
        data.add(createNewDeviceData(2, TIMESTAMP_NOW));

        final Integer lightIntegral = BedLightIntensity.integrateLight(data);
        assertThat(lightIntegral, is(6));
    }

    @Test
    public void testNightToMorningRatio() {
        final Integer nightSum = 1;
        final Integer morningSum = 10;
        final Float nightRatio = BedLightIntensity.getNightToMorningRatio(nightSum, morningSum);
        assertThat(nightRatio, is(0.1f));

        final Integer nightSum1 = 1;
        final Integer morningSum1 = 1;
        final Float nightRatio1 = BedLightIntensity.getNightToMorningRatio(nightSum1, morningSum1);
        assertThat(nightRatio1, is(1.0f));

        final Integer nightSum2 = 10;
        final Integer morningSum2 = 1;
        final Float nightRatio2 = BedLightIntensity.getNightToMorningRatio(nightSum2, morningSum2);
        assertThat(nightRatio2, is(10.0f));
    }

    /*
    @Test
    public void testNightToMorningLogTransform() {
        final Float nightRatio = 0.001f;
        final Integer nightRatioLog = BedLightIntensity.getNightToMorningRatioLogTransform(nightRatio);
        assertThat(nightRatioLog, is(-7));

        final Float nightRatio1 = 0.01f;
        final Integer nightRatioLog1 = BedLightIntensity.getNightToMorningRatioLogTransform(nightRatio1);
        assertThat(nightRatioLog1, is(-5));

        final Float nightRatio2 = 0.1f;
        final Integer nightRatioLog2 = BedLightIntensity.getNightToMorningRatioLogTransform(nightRatio2);
        assertThat(nightRatioLog2, is(-2));

        final Float nightRatio3 = 1.0f;
        final Integer nightRatioLog3 = BedLightIntensity.getNightToMorningRatioLogTransform(nightRatio3);
        assertThat(nightRatioLog3, is(0));

        final Float nightRatio4 = 10.0f;
        final Integer nightRatioLog4 = BedLightIntensity.getNightToMorningRatioLogTransform(nightRatio4);
        assertThat(nightRatioLog4, is(2));

        final Float nightRatio5 = 100.0f;
        final Integer nightRatioLog5 = BedLightIntensity.getNightToMorningRatioLogTransform(nightRatio5);
        assertThat(nightRatioLog5, is(5));

        final Float nightRatio6 = 1000.0f;
        final Integer nightRatioLog6 = BedLightIntensity.getNightToMorningRatioLogTransform(nightRatio6);
        assertThat(nightRatioLog6, is(7));
    }
    */

    @Test
    public void scoreInsightCardGood() {
        final Float nightRatio = 0.5f;

        final Optional<InsightCard> someResult = BedLightIntensity.scoreInsightCard(FAKE_ACCOUNT_ID, nightRatio);
        final String expectedTitle = BedLightIntensityMsgEN.getGoodHabits(0).title;
//        System.out.print(someResult.get().title + "\n");
//        System.out.print(someResult.get().message + "\n");
        assertThat(someResult.get().title, is(expectedTitle));
    }

    @Test
    public void scoreInsightCardBadOne() {
        final Float nightRatio = 1.01f;

        final Optional<InsightCard> someResult = BedLightIntensity.scoreInsightCard(FAKE_ACCOUNT_ID, nightRatio);
        final String expectedTitle = BedLightIntensityMsgEN.getMoreThanOne(0).title;
//        System.out.print(someResult.get().title + "\n");
//        System.out.print(someResult.get().message + "\n");
        assertThat(someResult.get().title, is(expectedTitle));
    }

    @Test
    public void scoreInsightCardBadTwo() {
        final Float nightRatio = 2.1f;

        final Optional<InsightCard> someResult = BedLightIntensity.scoreInsightCard(FAKE_ACCOUNT_ID, nightRatio);
        final String expectedTitle = BedLightIntensityMsgEN.getMoreThanTwo(0).title;
//        System.out.print(someResult.get().title + "\n");
//        System.out.print(someResult.get().message + "\n");
        assertThat(someResult.get().title, is(expectedTitle));
    }

    @Test
    public void scoreInsightCardBadThree() {
        final Float nightRatio = 3.2f;

        final Optional<InsightCard> someResult = BedLightIntensity.scoreInsightCard(FAKE_ACCOUNT_ID, nightRatio);
        final String expectedTitle = BedLightIntensityMsgEN.getMoreThanThree(0).title;
//        System.out.print(someResult.get().title + "\n");
//        System.out.print(someResult.get().message + "\n");
        assertThat(someResult.get().title, is(expectedTitle));
    }

    private static DeviceData createNewDeviceData(final Integer lightValue, final DateTime timestamp) {
        final Long FAKE_ACCOUNT_ID = 1999L;
        final Long FAKE_DEVICE_ID = 1998L;
        final Integer OFFSET_MILLIS_SF = -28800000;
        return new DeviceData(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, 0, 0, 0, 0, 0, 0, 0, lightValue, 0, 0, 0, timestamp, OFFSET_MILLIS_SF, 0, 0, 0, 0, 0, 0);
    }
}

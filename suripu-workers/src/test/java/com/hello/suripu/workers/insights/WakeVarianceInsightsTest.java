package com.hello.suripu.workers.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.WakeVarianceMsgEN;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.core.processors.insights.WakeVariance;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 7/29/15.
 */
public class WakeVarianceInsightsTest {

    @Test
    public void testNoCardGenerated() {
        final Long accountId = 999L;
        final List<Integer> wakeTimeList = new ArrayList<>();
        final Optional<InsightCard> noResult = WakeVariance.processWakeVarianceData(accountId, wakeTimeList, new WakeStdDevData());
        assertThat(noResult.isPresent(), is(Boolean.FALSE));
    }


    @Test
    public void testVarianceLow() {
        final Long accountId = 999L;
        final List<Integer> wakeTimeList = new ArrayList<>();
        final Integer spread = 1;
        wakeTimeList.add(0);
        wakeTimeList.add(0 + spread);
        wakeTimeList.add(0 + spread + spread);
        final Optional<InsightCard> someResult = WakeVariance.processWakeVarianceData(accountId, wakeTimeList, new WakeStdDevData());
        assertThat(someResult.isPresent(), is(Boolean.TRUE));
        final String expectedTitle = WakeVarianceMsgEN.getWakeVarianceLow(0, 0).title;
        assertThat(someResult.get().title, is(expectedTitle));

        final String someMessage = WakeVarianceMsgEN.getWakeVarianceLow(27,25).message;
        assertThat(someMessage, is(someMessage));
    }

    @Test
    public void testVarianceNotLowEnough() {
        final String someMessage = WakeVarianceMsgEN.getWakeVarianceNotLowEnough(50, 50).message;
        assertThat(someMessage, is(someMessage));
    }

    @Test
    public void testVarianceHigh() {
        final String someMessage = WakeVarianceMsgEN.getWakeVarianceHigh(79, 75).message;
        assertThat(someMessage, is(someMessage));
    }

    @Test
    public void testVarianceTooHigh() {
        final Long accountId = 999L;
        final List<Integer> wakeTimeList = new ArrayList<>();
        final Integer spread = 500;
        wakeTimeList.add(0);
        wakeTimeList.add(0 + spread);
        wakeTimeList.add(0 + spread + spread);
        final Optional<InsightCard> someResult = WakeVariance.processWakeVarianceData(accountId, wakeTimeList, new WakeStdDevData());
        assertThat(someResult.isPresent(), is(Boolean.TRUE));
        final String expectedTitle = WakeVarianceMsgEN.getWakeVarianceTooHigh(0, 0).title;
        assertThat(someResult.get().title, is(expectedTitle));

        final String someMessage = WakeVarianceMsgEN.getWakeVarianceTooHigh(200,99).message;
        assertThat(someMessage, is(someMessage));
    }

    @Test
    public void testMinHourConversion() {
        final int minOne = 30;
        final float hrOne = (float) minOne/60;
        final String stringOne = String.format("%.1f", hrOne);
        assertThat(stringOne, is("0.5"));

        final int minTwo = 60;
        final float hrTwo = (float) minTwo/60;
        final String stringTwo = String.format("%.1f", hrTwo);
        assertThat(stringTwo, is("1.0"));

        final int minThree = 90;
        final float hrThree = (float) minThree/60;
        final String stringThree = String.format("%.1f", hrThree);
        assertThat(stringThree, is("1.5"));
    }
}

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
        final List<Long> wakeTimeList = new ArrayList<>();
        final Optional<InsightCard> noResult = WakeVariance.processWakeVarianceData(accountId, wakeTimeList, new WakeStdDevData());
        assertThat(noResult.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void testVarianceTooHigh() {
        final Long accountId = 999L;
        final List<Long> wakeTimeList = new ArrayList<>();
        final Long spread = 500L;
        wakeTimeList.add(0L);
        wakeTimeList.add(0L + spread);
        wakeTimeList.add(0L + spread + spread);
        final Optional<InsightCard> someResult = WakeVariance.processWakeVarianceData(accountId, wakeTimeList, new WakeStdDevData());
        assertThat(someResult.isPresent(), is(Boolean.TRUE));
        final String expectedTitle = WakeVarianceMsgEN.getWakeVarianceTooHigh(0, 0).title;
        assertThat(someResult.get().title, is(expectedTitle));
    }

    @Test
    public void testVarianceLow() {
        final Long accountId = 999L;
        final List<Long> wakeTimeList = new ArrayList<>();
        final Long spread = 1L;
        wakeTimeList.add(0L);
        wakeTimeList.add(0L + spread);
        wakeTimeList.add(0L + spread + spread);
        final Optional<InsightCard> someResult = WakeVariance.processWakeVarianceData(accountId, wakeTimeList, new WakeStdDevData());
        assertThat(someResult.isPresent(), is(Boolean.TRUE));
        final String expectedTitle = WakeVarianceMsgEN.getWakeVarianceLow(0, 0).title;
        assertThat(someResult.get().title, is(expectedTitle));
    }

    @Test
    public void testVarianceNotLowEnough() {
        final Long accountId = 999L;
        final List<Long> wakeTimeList = new ArrayList<>();
        final Long spread = 50L;
        wakeTimeList.add(0L);
        wakeTimeList.add(0L + spread);
        wakeTimeList.add(0L + spread + spread);
        final Optional<InsightCard> someResult = WakeVariance.processWakeVarianceData(accountId, wakeTimeList, new WakeStdDevData());
        assertThat(someResult.isPresent(), is(Boolean.TRUE));
        final String expectedTitle = WakeVarianceMsgEN.getWakeVarianceNotLowEnough(0,0).title;
        assertThat(someResult.get().title, is(expectedTitle));
    }

}

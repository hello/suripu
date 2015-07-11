package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.*;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.WakeVarianceMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.processors.TimelineProcessor;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jingyun on 7/25/15.
 */

public class WakeVariance {
    public static Optional<InsightCard> getInsights(final TimelineProcessor timelineProcessor, final Long accountId, final WakeStdDevData wakeStdDevData, final DateTime queryEndDate, final int numDays) {

        final List<DateTime> dateTimeList = new ArrayList<>();
        for (int i = 1; i < numDays; i++) {
            dateTimeList.add(queryEndDate.minusDays(i));
        }

        //get wake variance data for the past n=numDays days
        final List<TimelineResult> timelineResultList = timelineProcessor.retrieveTimelinesListFast(accountId, dateTimeList);
        final List<Long> wakeTimeList = timelineProcessor.getEventDiffFromLocalStartOfDayList(timelineResultList, Event.Type.WAKE_UP);

        final Optional<InsightCard> card = processWakeVarianceData(accountId, wakeTimeList, wakeStdDevData);
        return card;
    }

    public static Optional<InsightCard> processWakeVarianceData(final Long accountId, final List<Long> wakeTimeList, final WakeStdDevData wakeStdDevData) {

        if (wakeTimeList.isEmpty()) {
            return Optional.absent();
        }
        else if (wakeTimeList.size() <= 2) {
            return Optional.absent(); //not big enough to calculate variance usefully
        }

        // compute variance
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final long wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        double wakeStdDev = 99;
        if (stats.getN() > 2) {
            wakeStdDev = stats.getStandardDeviation();
        }

        final Integer percentile = wakeStdDevData.getWakeStdDevPercentile(wakeStdDev);

        Text text;
        if (wakeStdDev <= 36) {
            text = WakeVarianceMsgEN.getWakeVarianceLow(wakeStdDev, percentile);
        }
        else if (wakeStdDev <= 79) {
            text = WakeVarianceMsgEN.getWakeVarianceNotLowEnough(wakeStdDev, percentile);
        }
        else if (wakeStdDev <= 122) {
            text = WakeVarianceMsgEN.getWakeVarianceHigh(wakeStdDev, percentile);
        }
        else {
            text = WakeVarianceMsgEN.getWakeVarianceTooHigh(wakeStdDev, percentile);
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.WAKE_VARIANCE, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC)));

    }

}

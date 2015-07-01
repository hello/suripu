package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.models.*;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.WakeVarianceMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.processors.TimelineProcessor;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jingyun on 7/25/15.
 */

//TODO: Plot distribution of wakeVariances and pull different percentiles, and then with more time, do cluster analysis to get better threshold cutoffs
public class WakeVariance {
    public static Optional<InsightCard> getInsights(final TimelineProcessor timelineProcessor, final Long accountId, final WakeVarianceData wakeVarianceData, final DateTime queryEndDate, final int numDays) {

        final List<DateTime> dateTimeList = new ArrayList<>();
        for (int i = 1; i < numDays; i++) {
            dateTimeList.add(queryEndDate.minusDays(i));
        }

        //get wake variance data for the past n=-numDays days
        final List<TimelineResult> timelineResultList = timelineProcessor.retrieveTimelinesListFast(accountId, dateTimeList);
        final List<Long> wakeTimeList = timelineProcessor.getEventDiffFromLocalStartOfDayList(timelineResultList, Event.Type.WAKE_UP);

        final Optional<InsightCard> card = processWakeVarianceData(accountId, wakeTimeList, wakeVarianceData);
        return card;
    }

    public static Optional<InsightCard> processWakeVarianceData(final Long accountId, final List<Long> wakeTimeList, final WakeVarianceData wakeVarianceData) {

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

        double wakeVariance = 99;
        if (stats.getN() > 2) {
            wakeVariance = stats.getVariance();//May want to change to Suh et. al formula
        }

        final Integer percentile = wakeVarianceData.getWakeVariancePercentile(wakeVariance);

        // see: Suh et. al on Clinical Significance of night-to-night sleep variability in insomnia, also need to refine levels based on our data
        Text text;
        if (wakeVariance <= 1) {
            text = WakeVarianceMsgEN.getWakeVarianceLow(wakeVariance, percentile);
        }
        else if (wakeVariance <= 2) {
            text = WakeVarianceMsgEN.getWakeVarianceNotLowEnough(wakeVariance, percentile);
        }
        else if (wakeVariance <= 3) {
            text = WakeVarianceMsgEN.getWakeVarianceHigh(wakeVariance, percentile);
        }
        else {
            text = WakeVarianceMsgEN.getWakeVarianceTooHigh(wakeVariance, percentile);
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.WAKE_VARIANCE, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC)));

    }

}

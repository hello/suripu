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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jingyun on 7/25/15.
 */

//TODO: Plot distribution of wakeVariances and pull different percentiles, and then with more time, do cluster analysis to get better threshold cutoffs
public class WakeVariance {
    public static Optional<InsightCard> getInsights(final TimelineProcessor timelineProcessor, final Long accountId, final WakeVarianceData wakeVarianceData) {

        // get wake variance data for last week (7 days)
        final DateTime dateTimeList[] = new DateTime[7];
        //final List<TimelineResult> timelineResultList = new ArrayList<>();
        final long wakeTimeList[] = new long[dateTimeList.length];

        final DateTime queryEndDate = dateTimeList[0] = DateTime.now(DateTimeZone.UTC);
        int i;
        for (i = 1; i < dateTimeList.length; i++) {
            dateTimeList[i] = queryEndDate.minusDays(i);

            final Optional<TimelineResult> optionalTimeline = timelineProcessor.retrieveTimelinesFast(accountId, dateTimeList[i]);
            if (optionalTimeline.isPresent()) {
                final TimelineResult timelineResult = optionalTimeline.get();
                for (SleepSegment event : timelineResult.timelines.get(0).events) { //need to get(0) because TimelineResult allows for multiple timeline in a single day
                    if (event.getType() == Event.Type.WAKE_UP) {
                        DateTime wakeDateTime = new DateTime(event.getTimestamp(), DateTimeZone.forOffsetMillis(event.getOffsetMillis()));
                        wakeTimeList[i] = wakeDateTime.getMillis() - wakeDateTime.withTimeAtStartOfDay().getMillis(); //get difference in millis between wake time and start of day time
                    }
                }
            }
        }

        final Optional<InsightCard> card = processWakeVarianceData(accountId, wakeTimeList, wakeVarianceData);
        return card;
    }

    public static Optional<InsightCard> processWakeVarianceData(final Long accountId, final long[] wakeTimeList, final WakeVarianceData wakeVarianceData) {

        if (wakeTimeList.length == 0) return Optional.absent();

        // compute variance
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final long wakeTime : wakeTimeList) {
            stats.addValue(wakeTime); //what is type/properties of timelineResult. See above
        }

        int wakeVariance = 99;
        if (stats.getN() >= 2) {
            wakeVariance = (int) stats.getVariance();//May want to change to Suh et. al formula
        }

        final int percentile = wakeVarianceData.getWakeVariancePercentile(wakeVariance);

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

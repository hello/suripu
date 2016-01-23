package com.hello.suripu.core.trends.v2;


import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class TrendsProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrendsProcessor.class);

    private static final int MIN_DATA_SIZE = 7; // don't show annotation if less than this number of datapoints


    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final AccountDAO accountDAO;
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;


    public TrendsProcessor(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final AccountDAO accountDAO, final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB) {
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.accountDAO = accountDAO;
        this.timeZoneHistoryDAODynamoDB = timeZoneHistoryDAODynamoDB;
    }

    public TrendsResult getAllTrends(final Long accountId, final TimeScale timescale) {

        // get data
        final List<AggregateSleepStats> data = getRawData(accountId, TimeScale.TIMESCALE_MAP.get(timescale));

        if (data.isEmpty()) {
            return new TrendsResult(Collections.<TimeScale>emptyList(), Collections.<Graph>emptyList());
        }

        final List<Graph> graphs = Lists.newArrayList();

        // sleep-score grid graph
        final Optional<Graph> sleepScoreGraph = getSleepScoreGraph(data, timescale);
        if (sleepScoreGraph.isPresent()) {
            graphs.add(sleepScoreGraph.get());
        }

        // sleep duration bar graph
        final Optional<Graph> durationGraph = getSleepDurationGraph(data);
        if (durationGraph.isPresent()) {
            graphs.add(durationGraph.get());
        }

        // sleep depth bubbles
        final Optional<Graph> depthGraph = getSleepDepthGraph(data);
        if (depthGraph.isPresent()) {
            graphs.add(depthGraph.get());
        }

        // check account-age to determine available time-scale
        final List<TimeScale> timeScales = computeAvailableTimeScales(accountId);

        return new TrendsResult(timeScales, graphs);
    }


    private Optional<Graph> getSleepDepthGraph(List<AggregateSleepStats> data) {
        return Optional.absent();
    }

    private Optional<Graph> getSleepDurationGraph(List<AggregateSleepStats> data) {
        return Optional.absent();
    }

    private Optional<Graph> getSleepScoreGraph(final List<AggregateSleepStats> data, final TimeScale timeScale) {

        // computing averages
        TrendsProcessorUtils.AnnotationStats annotationStats = new TrendsProcessorUtils.AnnotationStats();
        final Boolean hasAnnotation = (data.size() >= MIN_DATA_SIZE);

        final List<Float> validData = Lists.newArrayList();
        float minValue = 100.0f;
        float maxValue = 0.0f;
        int dayOfWeek = 0;
        DateTime currentDateTime = data.get(0).dateTime;


        for (final AggregateSleepStats stat: data) {
            final float score = (float) stat.sleepScore;
            currentDateTime = stat.dateTime;
            dayOfWeek = currentDateTime.getDayOfWeek();

            final int currentIndex = data.indexOf(stat);

            // fill in missing gaps
            if (currentIndex > 0) {
                final DateTime previousDateTime = data.get(currentIndex - 1).dateTime;
                final Duration diff = new Duration(previousDateTime, currentDateTime);
                if (diff.getStandardDays() > 1) {
                    for (int day = 1; day < diff.getStandardDays(); day++) {
                        final int missingDay = previousDateTime.plusDays(day).getDayOfWeek();
                        validData.add(GraphSection.MISSING_VALUE);
                    }
                }
            }

            validData.add(score);

            if (hasAnnotation) {
                if (dayOfWeek <= DateTimeConstants.FRIDAY) {
                    annotationStats.sumWeekdayValues += score;
                    annotationStats.numWeekdays++;
                } else {
                    annotationStats.sumWeekendValues += score;
                    annotationStats.numWeekends++;
                }
                annotationStats.sumValues += score;
            }

            minValue = minValue < score ? minValue : score;
            maxValue = maxValue > score ? maxValue : score;

        }

        final DateTime today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final List<Float> sectionData = padSectionData(validData, today, data.get(0).dateTime, currentDateTime, TimeScale.TIMESCALE_MAP.get(timeScale));


        final List<GraphSection> sections = TrendsProcessorUtils.getSections(sectionData, DataType.SCORES, timeScale, today.getDayOfWeek());

        Optional<List<Annotation>> annotations = Optional.absent();
        if (hasAnnotation) {
            annotations = TrendsProcessorUtils.getAnnotations(annotationStats, DataType.SCORES);
        }

        // TODO: Condition Range

        final Graph graph = new Graph(
                timeScale,
                Graph.TITLE_SLEEP_SCORE,
                DataType.SCORES,
                GraphType.GRID,
                minValue,
                maxValue,
                sections,
                Optional.<List<ConditionRange>>absent(),
                annotations
                );

        return Optional.of(graph);
    }

    private List<Float> padSectionData(final List<Float> data, final DateTime today, final DateTime firstDataDateTime, final DateTime lastDataDateTime, final int numDays) {
        final List<Float> sectionData = Lists.newArrayList();

        // fill in missing days first
        final DateTime firstDate = today.minusDays(numDays);
        final int missingDays = (int) new Duration(firstDate, firstDataDateTime).getStandardDays();
        if (missingDays > 0) {
            for (int day = 0; day < missingDays; day ++) {
                sectionData.add(GraphSection.MISSING_VALUE);
            }
        }

        // pad front with nulls
        final int firstDateDOW = firstDate.getDayOfWeek();
        for (int day = 0; day < firstDateDOW; day ++) {
            sectionData.add(0, null);
        }

        sectionData.addAll(data);

        // add missing values at the end
        final int lastDataDOW = lastDataDateTime.getDayOfWeek();

        final int endMissingDays = (int) new Duration(lastDataDateTime, today.minusDays(1)).getStandardDays();
        if (endMissingDays > 0) {
            for (int day = 0; day < endMissingDays; day ++) {
                sectionData.add(GraphSection.MISSING_VALUE);
            }
        }

        // pad ends with nulls
        final int todayDOW = today.getDayOfWeek();
        if (todayDOW < DateTimeConstants.SUNDAY) {
            for (int day = todayDOW; day < DateTimeConstants.SUNDAY; day++) {
                sectionData.add(null);
            }
        }
        return sectionData;
    }

    private List<AggregateSleepStats> getRawData(final Long accountId, final int days) {
        final DateTime queryEnd = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final DateTime queryStart = queryEnd.minusDays(days);

        return sleepStatsDAODynamoDB.getBatchStats(accountId,
                DateTimeUtil.dateToYmdString(queryStart),
                DateTimeUtil.dateToYmdString(queryEnd));
    }

    private List<TimeScale> computeAvailableTimeScales(final Long accountId) {
        final Optional<Account> accountOptional = accountDAO.getById(accountId);
        final List<TimeScale> timeScales = Lists.newArrayList();

        if (accountOptional.isPresent()) {
            final int accountAge = DateTimeUtil.getDateDiffFromNowInDays(accountOptional.get().created);
            for (final TimeScale scale : TimeScale.TIMESCALE_MAP.keySet()) {
                if (accountAge >= TimeScale.TIMESCALE_MAP.get(scale)) {
                    timeScales.add(scale);
                }
            }
        }
        return timeScales;
    }

}

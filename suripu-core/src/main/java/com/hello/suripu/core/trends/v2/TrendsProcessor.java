package com.hello.suripu.core.trends.v2;


import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.models.timeline.v2.SleepState;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by ksg on 01/21/16
 */

public class TrendsProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrendsProcessor.class);

    private static final int MIN_ANNOTATION_DATA_SIZE = 7; // don't show annotation if less than this number of data-points
    private static final int MIN_DEPTH_DATA_SIZE = 7;
    private static final int MIN_SCORE_DATA_SIZE = 3;
    private static final int MIN_DURATION_DATA_SIZE = 7;

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
        final DateTime localToday = getLocalToday(accountId);
        final List<AggregateSleepStats> data = getRawData(accountId, localToday, timescale.getDays());

        if (data.isEmpty()) {
            LOGGER.debug("debug=no-trends-data, account={}", accountId);
            return new TrendsResult(Collections.<TimeScale>emptyList(), Collections.<Graph>emptyList());
        }

        final List<Graph> graphs = Lists.newArrayList();

        // sleep-score grid graph
        if (data.size() >= MIN_SCORE_DATA_SIZE) {
            final Optional<Graph> sleepScoreGraph = getDaysGraph(data, timescale, GraphType.GRID, DataType.SCORES, Graph.TITLE_SLEEP_SCORE, localToday);
            if (sleepScoreGraph.isPresent()) {
                graphs.add(sleepScoreGraph.get());
            }
        }

        // sleep duration bar graph
        if (data.size() >= MIN_DURATION_DATA_SIZE) {
            final Optional<Graph> durationGraph = getDaysGraph(data, timescale, GraphType.BAR, DataType.HOURS, Graph.TITLE_SLEEP_DURATION, localToday);
            if (durationGraph.isPresent()) {
                graphs.add(durationGraph.get());
            }
        }

        // sleep depth bubbles
        if (data.size() >= MIN_DEPTH_DATA_SIZE) {
            final Optional<Graph> depthGraph = getSleepDepthGraph(data, timescale);
            if (depthGraph.isPresent()) {
                graphs.add(depthGraph.get());
            }
        }

        // check account-age to determine available time-scale
        final List<TimeScale> timeScales = computeAvailableTimeScales(accountId);

        return new TrendsResult(timeScales, graphs);
    }


    private Optional<Graph> getSleepDepthGraph(final List<AggregateSleepStats> data, final TimeScale timeScale) {
        float totalSleep = 0.0f;
        float totalLightSleep = 0.0f;
        float totalSoundSleep = 0.0f;
        float totalMediumSleep = 0.0f;
        for (final AggregateSleepStats stat : data) {
            final float light = stat.sleepStats.lightSleepDurationInMinutes;
            totalLightSleep += light;

            final float sound = stat.sleepStats.soundSleepDurationInMinutes;
            totalSoundSleep += sound;

            final float medium = stat.sleepStats.sleepDurationInMinutes - light - sound;
            totalMediumSleep += medium;

            totalSleep += (light + sound + medium);
        }

        final List<GraphSection> sections = Lists.newArrayList();
        if (totalSleep > 0.0f) {
            final List<Float> sectionValues = Lists.newArrayList(
                    totalLightSleep/totalSleep,
                    totalMediumSleep/totalSleep,
                    totalSoundSleep/totalSleep);

            final List<String> title = Lists.newArrayList(SleepState.LIGHT.toString(),
                    SleepState.MEDIUM.toString(),
                    SleepState.SOUND.toString());

            sections.add(new GraphSection(sectionValues, Optional.of(title), Collections.<Integer>emptyList(), Optional.<Integer>absent()));
        }

        final Graph graph = new Graph(
                timeScale,
                Graph.TITLE_SLEEP_DEPTH,
                DataType.PERCENTS,
                GraphType.BUBBLES,
                0.0f,
                1.0f,
                sections,
                Optional.<List<ConditionRange>>absent(),
                Optional.<List<Annotation>>absent()
        );

        return Optional.of(graph);
    }

    /**
     * Get graphs where x-axis are days (Score and Sleep-Duration only)
     * @param data aggregate sleep-stats
     * @param timeScale look back days
     * @param graphType grid, bar or bubbles
     * @param dataType score, hours or percent
     * @param graphTitle name of the graph
     * @return Optional graph
     */
    private Optional<Graph> getDaysGraph(final List<AggregateSleepStats> data,
                                     final TimeScale timeScale,
                                     final GraphType graphType,
                                     final DataType dataType,
                                     final String graphTitle,
                                     final DateTime localToday) {

        TrendsProcessorUtils.AnnotationStats annotationStats = new TrendsProcessorUtils.AnnotationStats();
        final Boolean hasAnnotation = (data.size() >= MIN_ANNOTATION_DATA_SIZE);

        // computing averages
        final List<Float> validData = Lists.newArrayList();
        float minValue = 100.0f;
        float maxValue = 0.0f;
        DateTime currentDateTime = data.get(0).dateTime;

        for (final AggregateSleepStats stat: data) {
            float statValue;
            if (graphTitle.equals(Graph.TITLE_SLEEP_DURATION)) {
                statValue = (float) stat.sleepStats.sleepDurationInMinutes / 60.0f; // convert to hours
            } else {
                statValue = (float) stat.sleepScore;
            }

            currentDateTime = stat.dateTime;
            final int dayOfWeek = currentDateTime.getDayOfWeek();

            // fill in missing gaps
            final int currentIndex = data.indexOf(stat);
            if (currentIndex > 0) {
                final DateTime previousDateTime = data.get(currentIndex - 1).dateTime;
                final Days diffDays = Days.daysBetween(previousDateTime, currentDateTime);
                if (diffDays.getDays() > 1) {
                    for (int day = 1; day < diffDays.getDays(); day++) {
                        validData.add(GraphSection.MISSING_VALUE);
                    }
                }
            }

            validData.add(statValue);

            if (hasAnnotation) {
                if (dayOfWeek <= DateTimeConstants.FRIDAY) {
                    annotationStats.sumWeekdayValues += statValue;
                    annotationStats.numWeekdays++;
                } else {
                    annotationStats.sumWeekendValues += statValue;
                    annotationStats.numWeekends++;
                }
                annotationStats.sumValues += statValue;
                annotationStats.numDays++;
            }

            minValue = minValue < statValue ? minValue : statValue;
            maxValue = maxValue > statValue ? maxValue : statValue;

        }

        boolean padDayOfWeek = false;
        if (timeScale.equals(TimeScale.LAST_WEEK) || (dataType.equals(DataType.SCORES) && timeScale.equals(TimeScale.LAST_MONTH))) {
            padDayOfWeek = true;
        }

        final List<Float> sectionData = TrendsProcessorUtils.padSectionData(validData, localToday, data.get(0).dateTime, currentDateTime, timeScale.getDays(), padDayOfWeek);

        final List<GraphSection> sections = TrendsProcessorUtils.getScoreDurationSections(sectionData, minValue, maxValue, dataType, timeScale, localToday);

        Optional<List<Annotation>> annotations = Optional.absent();
        if (hasAnnotation) {
            annotations = TrendsProcessorUtils.getAnnotations(annotationStats, dataType);
        }

        Optional<List<ConditionRange>> conditionRanges = Optional.absent();
        if (graphTitle.equals(Graph.TITLE_SLEEP_SCORE)) {
            // only score has condition ranges for now
            conditionRanges = ConditionRange.getSleepScoreConditionRanges(minValue, maxValue);
        }

        final Graph graph = new Graph(
                timeScale,
                graphTitle,
                dataType,
                graphType,
                minValue,
                maxValue,
                sections,
                conditionRanges,
                annotations
        );

        return Optional.of(graph);
    }

    private List<AggregateSleepStats> getRawData(final Long accountId, final DateTime localToday, final int days) {
        final DateTime queryStart = localToday.minusDays(days);

        return sleepStatsDAODynamoDB.getBatchStats(accountId,
                DateTimeUtil.dateToYmdString(queryStart),
                DateTimeUtil.dateToYmdString(localToday));
    }

    private DateTime getLocalToday(final Long accountId) {
        final Optional<TimeZoneHistory> optionalTimeZone = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        if (optionalTimeZone.isPresent()) {
            final int offsetMillis = optionalTimeZone.get().offsetMillis;
            return DateTime.now(DateTimeZone.UTC).plusMillis(offsetMillis).withTimeAtStartOfDay();
        }
        return DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
    }

    private List<TimeScale> computeAvailableTimeScales(final Long accountId) {
        final Optional<Account> accountOptional = accountDAO.getById(accountId);
        final List<TimeScale> timeScales = Lists.newArrayList();

        if (accountOptional.isPresent()) {
            final int accountAge = DateTimeUtil.getDateDiffFromNowInDays(accountOptional.get().created);
            for (final TimeScale scale : TimeScale.values()) {
                if (accountAge >= scale.getDays()) {
                    timeScales.add(scale);
                }
            }
        }
        return timeScales;
    }

}

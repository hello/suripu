package com.hello.suripu.core.trends.v2;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.translations.English;
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

    public static final int ABSOLUTE_MIN_DATA_SIZE = 3;
    private static final int MIN_ACCOUNT_AGE = 3; // accounts that are less than 3 days old will not see graphs
    public static final int MAX_ACCOUNT_AGE_SHOW_WELCOME_CARDS = 14; //
    private static final int MIN_DATA_SIZE_SHOW_MINMAX = 3;
    private static final int MIN_VALID_SLEEP_DURATION = 30; // minutes

    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final AccountDAO accountDAO;
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;


    public static class TrendsData {
        private final DateTime dateTime;
        private final Integer sleepDurationInMinutes;
        private final Integer lightSleepInMinutes;
        private final Integer mediumSleepInMinutes;
        private final Integer soundSleepInMinutes;
        private final Integer sleepScore;

        public TrendsData(final DateTime dateTime,
                          final Integer sleepDurationInMinutes, final Integer lightSleepInMinutes,
                          final Integer mediumSleepInMinutes, final Integer soundSleepInMinutes,
                          final Integer sleepScore) {
            this.dateTime = dateTime;
            this.sleepDurationInMinutes = sleepDurationInMinutes;
            this.lightSleepInMinutes = lightSleepInMinutes;
            this.mediumSleepInMinutes = mediumSleepInMinutes;
            this.soundSleepInMinutes = soundSleepInMinutes;
            this.sleepScore = sleepScore;
        }

        public Integer getScore() { return this.sleepScore; }
        public Integer getDuration() { return this.sleepDurationInMinutes; }
    }

    public TrendsProcessor(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final AccountDAO accountDAO, final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB) {
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.accountDAO = accountDAO;
        this.timeZoneHistoryDAODynamoDB = timeZoneHistoryDAODynamoDB;
    }

    public TrendsResult getAllTrends(final Long accountId, final TimeScale timescale) {

        // get user's local today date
        final Integer offsetMillis = getAccountMillisOffset(accountId);
        final DateTime localToday = getLocalToday(offsetMillis);

        // compute account-age
        final Optional<Account> optionalAccount = accountDAO.getById(accountId);

        final Optional<DateTime> optionalAccountCreated;
        final int accountAge;
        if (optionalAccount.isPresent()) {
            final DateTime accountCreated = optionalAccount.get().created.plusMillis(offsetMillis).withTimeAtStartOfDay();
            final Days daysDiff = Days.daysBetween(accountCreated, localToday);
            accountAge = daysDiff.getDays() + 1;
            optionalAccountCreated = Optional.of(accountCreated);
        } else {
            accountAge = 0;
            optionalAccountCreated = Optional.absent();
            LOGGER.debug("key=trends-v2 action=no-account found account={}", accountId);
        }

        // get data
        final List<TrendsData> data = getRawData(accountId, localToday, timescale.getDays());

        LOGGER.debug("key=trends-v2 action=get-trends-graph account={} timescale={}, account_age={} local_today={}",
                accountId, timescale.toString(), accountAge, localToday);

        // all the graphs!
        return getGraphs(accountId, accountAge, optionalAccountCreated, localToday, timescale, data);
    }

    @VisibleForTesting
    protected TrendsResult getGraphs(final Long accountId, final Integer accountAge, final Optional<DateTime> optionalAccountCreated,
                                     final DateTime localToday, final TimeScale timescale, final List<TrendsData> data) {

        // accounts will only start seeing graphs on 4th day of account-creation
        if (accountAge <= MIN_ACCOUNT_AGE) {
            LOGGER.debug("key=trends-v2 action=fail-minimum-account-age-check account={} account_age={}", accountId, accountAge);
            return new TrendsResult(Collections.<TimeScale>emptyList(), Collections.<Graph>emptyList());
        }

        // check account-age and data to determine available time-scale to show
        final List<TimeScale> timeScales = computeAvailableTimeScales(accountAge);

        // no data no graphs
        if (data.isEmpty()) {
            // TODO: until we have a better way to check for number of data-points the account has....
            if (accountAge <= MAX_ACCOUNT_AGE_SHOW_WELCOME_CARDS) {
                LOGGER.debug("key=trends-v2 action=new-account-no-data-no-graph account={} account_age={}", accountId, accountAge);
                return new TrendsResult(Collections.<TimeScale>emptyList(), Collections.<Graph>emptyList());
            }

            LOGGER.debug("key=trends-v2 action=old-account-no-data-no-graph, account={}, num_timescales={}, account_age={}",
                    accountId, timeScales.size(), accountAge);
            return new TrendsResult(timeScales, Collections.<Graph>emptyList());
        }

        // minimum data requirement
        if (data.size() < ABSOLUTE_MIN_DATA_SIZE) {
            LOGGER.debug("key=trends-v2 action=insufficient-data-no-graphs account={} timescale={} data_size={}",
                    accountId, timescale.toString(), data.size());
            return new TrendsResult(timeScales, Collections.<Graph>emptyList());
        }

        // get graphs
        final List<Graph> graphs = Lists.newArrayList();

        boolean hasAnnotation = (accountAge >= Annotation.ANNOTATION_ENABLED_THRESHOLD);

        // sleep-score calendar
        final Optional<Graph> sleepScoreGraph = getDaysGraph(accountId, data, timescale,
                GraphType.GRID,
                DataType.SCORES,
                English.GRAPH_TITLE_SLEEP_SCORE,
                localToday, hasAnnotation, optionalAccountCreated);

        if (sleepScoreGraph.isPresent()) {
            graphs.add(sleepScoreGraph.get());
        }

        // sleep duration bar graph
        final Optional<Graph> durationGraph = getDaysGraph(accountId, data, timescale,
                GraphType.BAR,
                DataType.HOURS,
                English.GRAPH_TITLE_SLEEP_DURATION,
                localToday, hasAnnotation, optionalAccountCreated);

        if (durationGraph.isPresent()) {
            graphs.add(durationGraph.get());
        }

        // sleep depth bubbles
        final Optional<Graph> depthGraph = getSleepDepthGraph(data, timescale);
        if (depthGraph.isPresent()) {
            graphs.add(depthGraph.get());
        }

        LOGGER.debug("key=trends-v2 action==trends-graph-returned account={} num_timescales={} num_graphs={}",
                accountId, timeScales.size(), graphs.size());

        return new TrendsResult(timeScales, graphs);
    }


    private Optional<Graph> getSleepDepthGraph(final List<TrendsData> data, final TimeScale timeScale) {
        float totalSleep = 0.0f;
        float totalLightSleep = 0.0f;
        float totalSoundSleep = 0.0f;
        float totalMediumSleep = 0.0f;
        for (final TrendsData stat : data) {
            final float light = stat.lightSleepInMinutes;
            totalLightSleep += light;

            final float sound = stat.soundSleepInMinutes;
            totalSoundSleep += sound;

            final float medium = stat.mediumSleepInMinutes;
            totalMediumSleep += medium;

            totalSleep += (light + sound + medium);
        }

        final List<GraphSection> sections = Lists.newArrayList();
        if (totalSleep > 0.0f) {
            final List<Float> sectionValues = Lists.newArrayList(
                    totalLightSleep/totalSleep,
                    totalMediumSleep/totalSleep,
                    totalSoundSleep/totalSleep);

            final List<String> title = Lists.newArrayList(
                    English.SLEEP_DEPTH_LIGHT,
                    English.SLEEP_DEPTH_MEDIUM,
                    English.SLEEP_DEPTH_SOUND);

            sections.add(new GraphSection(sectionValues, title, Collections.<Integer>emptyList(), Optional.<Integer>absent()));
        }

        final Graph graph = new Graph(
                timeScale,
                English.GRAPH_TITLE_SLEEP_DEPTH,
                DataType.PERCENTS,
                GraphType.BUBBLES,
                0.0f,
                1.0f,
                sections,
                Collections.<ConditionRange>emptyList(),
                Collections.<Annotation>emptyList()
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
    @VisibleForTesting
    protected Optional<Graph> getDaysGraph(final long accountId,
                                           final List<TrendsData> data,
                                           final TimeScale timeScale,
                                           final GraphType graphType,
                                           final DataType dataType,
                                           final String graphTitle,
                                           final DateTime localToday,
                                           final boolean hasAnnotation,
                                           final Optional<DateTime> optionalCreated) {

        final TrendsProcessorUtils.AnnotationStats annotationStats = new TrendsProcessorUtils.AnnotationStats();

        // computing averages
        final List<Float> validData = Lists.newArrayList();
        float maxValue = Float.MIN_VALUE;
        float minValue = Float.MAX_VALUE;
        DateTime currentDateTime = data.get(0).dateTime;

        for (final TrendsData stat: data) {
            final float statValue;
            if (dataType.equals(DataType.HOURS)) {
                statValue = (float) stat.sleepDurationInMinutes / 60.0f; // convert to hours
            } else {
                statValue = (float) stat.sleepScore;
            }

            LOGGER.debug("key=trends-v2 action=aggregate-data, account={} date={}, stat={} value={}",
                    accountId, stat.dateTime, dataType.value, statValue);

            currentDateTime = stat.dateTime;
            final int dayOfWeek = currentDateTime.getDayOfWeek();

            // fill in missing gaps
            final int currentIndex = data.indexOf(stat);
            if (currentIndex > 0) {
                final DateTime previousDateTime = data.get(currentIndex - 1).dateTime;
                final Days diffDays = Days.daysBetween(previousDateTime, currentDateTime);
                if (diffDays.getDays() > 1) {
                    LOGGER.debug("key=trends-v2 action=missing-days account={} start={} end={} days={}",
                            accountId, previousDateTime, currentDateTime, diffDays.getDays());
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

            minValue = Math.min(minValue, statValue);
            maxValue = Math.max(maxValue, statValue);

        }
        LOGGER.debug("key=trends-v2 action=last-data-date account={} date={} today={} date-size={}",
                accountId, currentDateTime, localToday, validData.size());

        final boolean padDayOfWeek = (timeScale.equals(TimeScale.LAST_WEEK) || (dataType.equals(DataType.SCORES) && timeScale.equals(TimeScale.LAST_MONTH)));

        final List<Float> sectionData = TrendsProcessorUtils.padSectionData(validData, localToday, data.get(0).dateTime, currentDateTime, timeScale, padDayOfWeek, optionalCreated);

        final boolean hasMinMaxValues = (data.size() >= MIN_DATA_SIZE_SHOW_MINMAX);
        if (!hasMinMaxValues) {
            LOGGER.debug("key=trends-v2 action=graph-has-no-min-max-highlights account={} data_size={} data_type={}",
                    accountId, data.size(), dataType.value);
            minValue = 0.0f;
        }

        final List<GraphSection> sections = TrendsProcessorUtils.getScoreDurationSections(sectionData, minValue, maxValue, dataType, timeScale, localToday, hasMinMaxValues);

        final List<Annotation> annotations = Lists.newArrayList();
        if (hasAnnotation) {
            annotations.addAll(TrendsProcessorUtils.getAnnotations(annotationStats, dataType));
        }

        final List<ConditionRange> conditionRanges = Lists.newArrayList();
        if (dataType.equals(DataType.SCORES)) {
            // only score has condition ranges for now
            conditionRanges.addAll(ConditionRange.SLEEP_SCORE_CONDITION_RANGES);
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

    private List<TrendsData> getRawData(final Long accountId, final DateTime localToday, final int days) {
        final DateTime queryStart = localToday.minusDays(days);

        final List<AggregateSleepStats> rawData = sleepStatsDAODynamoDB.getBatchStats(accountId,
                DateTimeUtil.dateToYmdString(queryStart),
                DateTimeUtil.dateToYmdString(localToday));

        final List<TrendsData> results = Lists.newArrayList();
        for (final AggregateSleepStats stat : rawData) {
            if (stat.sleepStats.sleepDurationInMinutes >= MIN_VALID_SLEEP_DURATION ) {
                results.add(new TrendsData(stat.dateTime,
                        stat.sleepStats.sleepDurationInMinutes,
                        stat.sleepStats.lightSleepDurationInMinutes,
                        stat.sleepStats.mediumSleepDurationInMinutes,
                        stat.sleepStats.soundSleepDurationInMinutes,
                        stat.sleepScore));
            }
        }
        return results;
    }

    private Integer getAccountMillisOffset(final Long accountId) {
        final Optional<TimeZoneHistory> optionalTimeZone = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        if (optionalTimeZone.isPresent()) {
            return optionalTimeZone.get().offsetMillis;
        }
        return 0;
    }

    private DateTime getLocalToday(final Integer offsetMillis) {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        if (offsetMillis != 0) {
            return now.plusMillis(offsetMillis).withTimeAtStartOfDay();
        }
        return now.withTimeAtStartOfDay();
    }

    @VisibleForTesting
    protected List<TimeScale> computeAvailableTimeScales(final int accountAge) {
        final List<TimeScale> timeScales = Lists.newArrayList();
        for (final TimeScale scale : TimeScale.values()) {
            if (accountAge > scale.getVisibleAfterDays()) {
                timeScales.add(scale);
            }
        }

        return timeScales;
    }

}

package com.hello.suripu.core.trends.v2;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by kingshy on 3/10/16.
 */
public class TestDurationGraphs {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDurationGraphs.class);

    private SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private AccountDAO accountDAO;
    private TrendsProcessor trendsProcessor;

    @Before
    public void setUp() throws Exception {
        this.sleepStatsDAODynamoDB = mock(SleepStatsDAODynamoDB.class);
        this.timeZoneHistoryDAODynamoDB = mock(TimeZoneHistoryDAODynamoDB.class);
        this.accountDAO = mock(AccountDAO.class);
        this.trendsProcessor = new TrendsProcessor(sleepStatsDAODynamoDB, accountDAO, timeZoneHistoryDAODynamoDB);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testDurationWeekMinMax() {
        // test week
        final DateTime today = new DateTime(DateTimeZone.UTC).withDate(2016, 3, 6).withTimeAtStartOfDay(); // Sun

        final List<TrendsProcessor.TrendsData> data = Lists.newArrayList(); // data in ascending order
        final int maxDurationMins = 600;
        final int minDurationMins = 300;
        final int yesterdayValue = 400;
        data.add(new TrendsProcessor.TrendsData(today.minusDays(5), maxDurationMins, maxDurationMins - 200, 100, 100, 50)); // max <- should be highlighted (2)
        data.add(new TrendsProcessor.TrendsData(today.minusDays(4), minDurationMins, minDurationMins - 200, 100, 100, 50)); // min (3)
        data.add(new TrendsProcessor.TrendsData(today.minusDays(3), minDurationMins, minDurationMins - 200, 100, 100, 50)); // min <- should be highlighted(4)
        data.add(new TrendsProcessor.TrendsData(today.minusDays(2), maxDurationMins, maxDurationMins - 200, 100, 100, 50)); // max (5)
        data.add(new TrendsProcessor.TrendsData(today.minusDays(1), yesterdayValue, 100, 100, 100, 50)); // yesterday, Sat (6)

        final int accountAge = 7; // should have 6 valid nights of data
        final DateTime accountCreated = today.minusDays(accountAge - 1);

        final boolean hasAnnotation = (accountAge >= Annotation.ANNOTATION_ENABLED_THRESHOLD);
        final Optional<Graph> optionalGraph = this.trendsProcessor.getDaysGraph(data,
                TimeScale.LAST_WEEK,
                GraphType.BAR,
                DataType.HOURS,
                English.GRAPH_TITLE_SLEEP_DURATION,
                today, hasAnnotation, Optional.of(accountCreated));

        assertThat(optionalGraph.isPresent(), is(true));

        final Graph graph = optionalGraph.get();
        assertThat(graph.annotations.size(), is(3));
        assertThat(graph.minValue, is((float) minDurationMins / 60.0f));
        assertThat(graph.maxValue, is((float) maxDurationMins / 60.0f));

        assertThat(graph.sections.size(), is(1));

        final GraphSection section = graph.sections.get(0);
        assertThat(section.highlightedValues.size(), is(2));
        assertThat(section.highlightedValues.get(0), is(4)); // highlight latest min
        assertThat(section.highlightedValues.get(1), is(2)); // highlight earliest max
        assertThat(section.highlightedTitle.get(), is(6));

        // check titles and values
        assertThat(section.titles.get(0).equalsIgnoreCase("SUN"), is(true));
        assertThat(section.titles.get(6).equalsIgnoreCase("SAT"), is(true));
        assertThat(section.values.get(0), is (GraphSection.MISSING_VALUE));
        assertThat(section.values.get(1), is(GraphSection.MISSING_VALUE));
        assertThat(section.values.get(6), is((float) yesterdayValue / 60.0f));
    }

    @Test
    public void testDurationWeekNoMinMax() {

        // only two nights of data in the past week, should not have min/max highlights
        final DateTime today = new DateTime(DateTimeZone.UTC).withDate(2016, 3, 6).withTimeAtStartOfDay(); // Sun

        final List<TrendsProcessor.TrendsData> data = Lists.newArrayList(); // data in ascending order
        final int maxDurationMins = 600;
        final int minDurationMins = 300;

        data.add(new TrendsProcessor.TrendsData(today.minusDays(5), maxDurationMins, maxDurationMins - 200, 100, 100, 50)); // max <- should be highlighted (2)
        data.add(new TrendsProcessor.TrendsData(today.minusDays(4), minDurationMins, minDurationMins - 200, 100, 100, 50)); // min (3)

        final int accountAge = 7;
        final DateTime accountCreated = today.minusDays(accountAge - 1);

        final boolean hasAnnotation = (accountAge >= Annotation.ANNOTATION_ENABLED_THRESHOLD);
        final Optional<Graph> optionalGraph = this.trendsProcessor.getDaysGraph(data,
                TimeScale.LAST_WEEK,
                GraphType.BAR,
                DataType.HOURS,
                English.GRAPH_TITLE_SLEEP_DURATION,
                today, hasAnnotation, Optional.of(accountCreated));

        assertThat(optionalGraph.isPresent(), is(true));
        final Graph graph = optionalGraph.get();

        assertThat(graph.sections.size(), is(1));
        assertThat(graph.sections.get(0).highlightedValues.size(), is(0));

    }
}

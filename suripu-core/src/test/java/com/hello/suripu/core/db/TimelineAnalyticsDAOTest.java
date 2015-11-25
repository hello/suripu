package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.db.mappers.GroupedTimelineLogsSummaryMapper;
import com.hello.suripu.core.models.GroupedTimelineLogSummary;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 10/5/15.
 */
public class TimelineAnalyticsDAOTest {

    private DBI dbi;
    private Handle handle;
    private TimelineAnalyticsDAO dao;

    @Before
    public void setUp() throws Exception {
        final String createTableQuery = "CREATE TABLE timeline_analytics (\n" +
                "    id BIGSERIAL PRIMARY KEY,\n" +
                "    account_id BIGINT,\n" +
                "    date_of_night VARCHAR,\n" +
                "    algorithm INTEGER,\n" +
                "    error INTEGER,\n" +
                "    created_at TIMESTAMP,\n" +
                "    test_group BIGINT\n" +
                ");";

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new GroupedTimelineLogsSummaryMapper());
        handle = dbi.open();

        handle.execute(createTableQuery);
        dao = dbi.onDemand(TimelineAnalyticsDAO.class);
    }

    @After
    public void tearDown() throws Exception {
        handle.execute("DROP TABLE timeline_analytics;");
        handle.close();
    }

    @Test
    public void testInsert() throws Exception {
        final long accountId = 1;
        final LoggingProtos.TimelineLog log = LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(accountId)
                .setTimestampWhenLogGenerated(1)
                .setNightOf("2015-10-01")
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_ERROR)
                .setTestGroup(0L)
                .build();
        dao.insert(log);
        List<Map<String, Object>> result = handle.select(String.format("SELECT account_id FROM timeline_analytics WHERE account_id=%d;", accountId));
        assertThat((Long) result.get(0).get("account_id"), is(accountId));
    }

    @Test
    public void testInsertBatchWithIndividualRetryEmptyList() throws Exception {
        final List<LoggingProtos.TimelineLog> logs = new ArrayList<>();
        dao.insertBatchWithIndividualRetry(logs);
        final List<Map<String, Object>> results = handle.select("SELECT COUNT(*) AS count FROM timeline_analytics;");
        assertThat((Long) results.get(0).get("count"), is(new Long(0)));
    }

    @Test
    public void testInsertBatchWithIndividualRetryAllSuccessful() throws Exception {
        final List<LoggingProtos.TimelineLog> logs = new ArrayList<>();
        logs.add(LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(1)
                .setTimestampWhenLogGenerated(1)
                .setNightOf("2015-10-01")
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_ERROR)
                .build());
        logs.add(LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(2)
                .setTimestampWhenLogGenerated(1)
                .setNightOf("2015-10-02")
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_ERROR)
                .build());
        dao.insertBatchWithIndividualRetry(logs);

        final List<Map<String, Object>> results = handle.select("SELECT * FROM timeline_analytics;");
        assertThat((String) results.get(0).get("date_of_night"), equalTo("2015-10-01"));
        assertThat((String) results.get(1).get("date_of_night"), equalTo("2015-10-02"));
    }

    @Test
    public void testInsertBatchWithIndividualRetrySomeFailures() throws Exception {
        // Force account_id to be unique so that we can ensure some failures.
        handle.execute("ALTER TABLE timeline_analytics ADD CONSTRAINT unique_account_id UNIQUE (account_id)");
        final List<LoggingProtos.TimelineLog> logs = new ArrayList<>();
        logs.add(LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(1)
                .setTimestampWhenLogGenerated(1)
                .setNightOf("2015-10-01")
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_ERROR)
                .build());
        logs.add(LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(1)
                .setTimestampWhenLogGenerated(1)
                .setNightOf("2015-10-02")
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_ERROR)
                .build());
        dao.insertBatchWithIndividualRetry(logs);

        final List<Map<String, Object>> results = handle.select("SELECT * FROM timeline_analytics;");
        assertThat((String) results.get(0).get("date_of_night"), equalTo("2015-10-01"));
        assertThat(results.size(), is(1));
    }

    @Test
    public void testGetGroupedSummary() throws Exception {
        final List<LoggingProtos.TimelineLog> logs = new ArrayList<>();
        logs.add(LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(1)
                .setTimestampWhenLogGenerated(1)
                .setNightOf("2015-10-01")
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_ERROR)
                .build());
        logs.add(LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(2)
                .setTimestampWhenLogGenerated(1)
                .setNightOf("2015-10-01")
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_ERROR)
                .build());
        logs.add(LoggingProtos.TimelineLog.newBuilder()
                .setAccountId(3)
                .setTimestampWhenLogGenerated(1)
                .setNightOf("2015-10-01")
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM)
                .setError(LoggingProtos.TimelineLog.ErrorType.EVENTS_OUT_OF_ORDER)
                .build());
        dao.insertBatchWithIndividualRetry(logs);

        final List<GroupedTimelineLogSummary> summaries = dao.getGroupedSummary("2015-10-01");
        GroupedTimelineLogSummary noErrorSummary = null;
        GroupedTimelineLogSummary eventsOutOfOrderSummary = null;
        for (final GroupedTimelineLogSummary summary : summaries) {
            if (summary.error == LoggingProtos.TimelineLog.ErrorType.NO_ERROR) {
                noErrorSummary = summary;
            } else if (summary.error == LoggingProtos.TimelineLog.ErrorType.EVENTS_OUT_OF_ORDER) {
                eventsOutOfOrderSummary = summary;
            }
        }
        assertThat(noErrorSummary.count, is(2));
        assertThat(eventsOutOfOrderSummary.count, is(1));
    }

    private Optional<GroupedTimelineLogSummary> findSummary(final List<GroupedTimelineLogSummary> summaries,
                                                            final LoggingProtos.TimelineLog.AlgType algType,
                                                            final LoggingProtos.TimelineLog.ErrorType errorType,
                                                            final String date) {
        for (final GroupedTimelineLogSummary summary : summaries) {
            if (summary.algorithm == algType &&
                    summary.error == errorType &&
                    summary.date == date) {
                return Optional.of(summary);
            }
        }
        return Optional.absent();
    }

    @Test
    public void testGetGroupedSummariesForDateRange() throws Exception {
        final List<LoggingProtos.TimelineLog> logs = new ArrayList<>();
        final long account1 = 1;
        final long account2 = 2;
        final long timestamp1 = 1;
        final long timestamp2 = 2;
        final String date1 = "2015-09-30";
        final String date2 = "2015-10-01";
        // date1
        final LoggingProtos.TimelineLog.Builder template = LoggingProtos.TimelineLog.newBuilder();
        template.setAccountId(account1)
                .setTimestampWhenLogGenerated(timestamp1)
                .setNightOf(date1)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_ERROR)
                .setAlgorithm(LoggingProtos.TimelineLog.AlgType.HMM);
        logs.add(template.build());
        logs.add(template
                .clone()
                .setError(LoggingProtos.TimelineLog.ErrorType.EVENTS_OUT_OF_ORDER)
                .build());
        // date2
        logs.add(template
                .clone()
                .setNightOf(date2)
                .setTimestampWhenLogGenerated(timestamp2)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_DATA)
                .build());
        logs.add(template
                .clone()
                .setNightOf(date2)
                .setAccountId(account2)
                .setError(LoggingProtos.TimelineLog.ErrorType.EVENTS_OUT_OF_ORDER)
                .build());
        logs.add(template
                .clone()
                .setAccountId(account2)
                .setNightOf(date2)
                .setError(LoggingProtos.TimelineLog.ErrorType.NO_DATA)
                .build());
        logs.add(template
                .clone()
                .setAccountId(account2)
                .setNightOf(date2)
                .setError(LoggingProtos.TimelineLog.ErrorType.NOT_ENOUGH_DATA)
                .build());
        dao.insertBatchWithIndividualRetry(logs);

        final List<GroupedTimelineLogSummary> groupedSummaries = dao.getGroupedSummariesForDateRange(date1, date2);
        assertThat(groupedSummaries.size(), is(5));
        assertThat(findSummary(groupedSummaries, LoggingProtos.TimelineLog.AlgType.HMM, LoggingProtos.TimelineLog.ErrorType.NO_ERROR, date1).get().count,
                    is(1));
        assertThat(findSummary(groupedSummaries, LoggingProtos.TimelineLog.AlgType.HMM, LoggingProtos.TimelineLog.ErrorType.EVENTS_OUT_OF_ORDER, date1).get().count,
                is(1));
        assertThat(findSummary(groupedSummaries, LoggingProtos.TimelineLog.AlgType.HMM, LoggingProtos.TimelineLog.ErrorType.NO_DATA, date2).get().count,
                is(2));
        assertThat(findSummary(groupedSummaries, LoggingProtos.TimelineLog.AlgType.HMM, LoggingProtos.TimelineLog.ErrorType.EVENTS_OUT_OF_ORDER, date2).get().count,
                is(1));
        assertThat(findSummary(groupedSummaries, LoggingProtos.TimelineLog.AlgType.HMM, LoggingProtos.TimelineLog.ErrorType.NOT_ENOUGH_DATA, date2).get().count,
                is(1));
    }
}
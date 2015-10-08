package com.hello.suripu.core.db;

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
                "    created_at TIMESTAMP\n" +
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
}
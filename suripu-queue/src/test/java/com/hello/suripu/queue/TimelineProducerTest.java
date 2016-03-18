package com.hello.suripu.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.queue.models.AccountData;
import com.hello.suripu.queue.models.SenseDataDAO;
import com.hello.suripu.queue.workers.TimelineQueueProducerManager;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ksg on 3/17/16
 */

public class TimelineProducerTest {
    private TimelineQueueProducerManager producerManager;
    private SenseDataDAO senseDataDAO;

    @Before
    public void setUp() throws Exception {
        senseDataDAO = mock(SenseDataDAO.class);
        final AmazonSQSAsync sqsClient = mock(AmazonSQSAsync.class);
        final String queueUrl = "testing";
        final ExecutorService executor = mock(ExecutorService.class);
        final ScheduledExecutorService producerExecutor = mock(ScheduledExecutorService.class);
        final int numProducerThreads = 1;

        this.producerManager = new TimelineQueueProducerManager(
                sqsClient,
                senseDataDAO,
                queueUrl,
                producerExecutor,
                executor,
                5,
                numProducerThreads);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testTargetDates() {
        final DateTime now = new DateTime(DateTimeZone.UTC).withDate(2016, 3, 17).withHourOfDay(23).withMinuteOfHour(8).withSecondOfMinute(5);

        final int gmt14OffsetMillis = 50400000;
        final List<AccountData> gmt14List = Lists.newArrayList();
        gmt14List.add(new AccountData(10L, gmt14OffsetMillis, DateTime.now(DateTimeZone.UTC).minusHours(10)));
        gmt14List.add(new AccountData(11L, gmt14OffsetMillis, DateTime.now(DateTimeZone.UTC).minusHours(10)));

        when(senseDataDAO.getValidAccounts(
                now.withTimeAtStartOfDay().minusDays(1),
                now.withTimeAtStartOfDay(), gmt14OffsetMillis)).thenReturn(ImmutableList.copyOf(gmt14List));

        final int gmtMinus11OffsetMillis = -36000000;
        final List<AccountData> gmtMinus11List = Lists.newArrayList();
        gmtMinus11List.add(new AccountData(20L, gmtMinus11OffsetMillis, DateTime.now(DateTimeZone.UTC).minusHours(11)));
        gmtMinus11List.add(new AccountData(21L, gmtMinus11OffsetMillis, DateTime.now(DateTimeZone.UTC).minusHours(11)));

        when(senseDataDAO.getValidAccounts(
                now.withTimeAtStartOfDay().minusDays(1),
                now.withTimeAtStartOfDay(), gmtMinus11OffsetMillis)).thenReturn(ImmutableList.copyOf(gmtMinus11List));

        final Map<Long, String> accountIds14 = this.producerManager.getCurrentTimezoneAccountIds(now, Lists.newArrayList(gmt14OffsetMillis));
        for (final String targetDate : accountIds14.values()) {
            assertThat(targetDate.equalsIgnoreCase("2016-03-17"), is(true));
        }

        final Map<Long, String> accountIds11 = this.producerManager.getCurrentTimezoneAccountIds(now, Lists.newArrayList(gmtMinus11OffsetMillis));
        for (final String targetDate : accountIds11.values()) {
            assertThat(targetDate.equalsIgnoreCase("2016-03-16"), is(true));
        }

    }
}

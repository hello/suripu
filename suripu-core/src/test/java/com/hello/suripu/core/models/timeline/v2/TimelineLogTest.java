package com.hello.suripu.core.models.timeline.v2;

import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.TimelineError;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class TimelineLogTest extends TestCase {

    private static final long testAccountId = 1;
    private static final long dateOfNight = 2;
    private static final long timestamp = 3;

    private void assertLogsHaveRequiredFields(final TimelineLog log) {
        for (final LoggingProtos.TimelineLog tlLog : log.build().getTimelineLogList()) {
            assertThat(tlLog.getAccountId(), is(testAccountId));
            assertThat(tlLog.getNightOfTimeline(), is(dateOfNight));
            assertThat(tlLog.getTimestampWhenLogGenerated(), is(timestamp));
        }
    }

    public void testAddMessageWithError() throws Exception {
        final TimelineLog log = new TimelineLog(testAccountId, dateOfNight, timestamp);
        log.addMessage(TimelineError.DATA_GAP_TOO_LARGE);
        final LoggingProtos.BatchLogMessage batchMessage = log.build();
        assertThat(log.build().getTimelineLogCount(), is(1));

        log.addMessage(TimelineError.EVENTS_OUT_OF_ORDER);
        assertThat(log.build().getTimelineLogCount(), is(2));

        assertLogsHaveRequiredFields(log);
        assertThat(log.build().getTimelineLog(0).getError(), is(LoggingProtos.TimelineLog.ErrorType.DATA_GAP_TOO_LARGE));
        assertThat(log.build().getTimelineLog(1).getError(), is(LoggingProtos.TimelineLog.ErrorType.EVENTS_OUT_OF_ORDER));
    }

    public void testAddMessageWithErrorAndMessage() throws Exception {
        final String message = "my message";
        final TimelineLog log = new TimelineLog(testAccountId, dateOfNight, timestamp);
        log.addMessage(TimelineError.DATA_GAP_TOO_LARGE, message);
        assertLogsHaveRequiredFields(log);
        assertThat(log.build().getTimelineLog(0).getMessage(), is(message));
    }

    public void testAddMessageWithAlgorithmTypeAndPredictions() throws Exception {
        final List<Event> predictions = new ArrayList<>();
        final Event event = new OutOfBedEvent(SleepPeriod.Period.NIGHT, 0, 0, 0);
        predictions.add(event);
        final TimelineLog log = new TimelineLog(testAccountId, dateOfNight, timestamp);
        log.addMessage(AlgorithmType.HMM, predictions);

        assertLogsHaveRequiredFields(log);
        assertThat(log.build().getTimelineLog(0).getAlgorithm(), is(LoggingProtos.TimelineLog.AlgType.HMM));
        assertThat(log.build().getTimelineLog(0).getPredictions(0).getEventType(), is(LoggingProtos.TimelineLog.SleepEventType.OUT_OF_BED));
    }

    public void testAddMessageWithAlgorithmTypeAndError() throws Exception {
        final TimelineLog log = new TimelineLog(testAccountId, dateOfNight, timestamp);
        log.addMessage(AlgorithmType.HMM, TimelineError.INVALID_SLEEP_SCORE);

        assertLogsHaveRequiredFields(log);
        assertThat(log.build().getTimelineLog(0).getAlgorithm(), is(LoggingProtos.TimelineLog.AlgType.HMM));
        assertThat(log.build().getTimelineLog(0).getError(), is(LoggingProtos.TimelineLog.ErrorType.INVALID_SLEEP_SCORE));
    }

    public void testAddMessageWithAlgorithmTypeErrorAndMessage() throws Exception {
        final String message = "lol";
        final TimelineLog log = new TimelineLog(testAccountId, dateOfNight, timestamp);
        log.addMessage(AlgorithmType.HMM, TimelineError.INVALID_SLEEP_SCORE, message);

        assertLogsHaveRequiredFields(log);
        assertThat(log.build().getTimelineLog(0).getAlgorithm(), is(LoggingProtos.TimelineLog.AlgType.HMM));
        assertThat(log.build().getTimelineLog(0).getError(), is(LoggingProtos.TimelineLog.ErrorType.INVALID_SLEEP_SCORE));
        assertThat(log.build().getTimelineLog(0).getMessage(), is(message));
    }
}
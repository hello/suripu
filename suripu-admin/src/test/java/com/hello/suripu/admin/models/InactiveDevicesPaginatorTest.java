package com.hello.suripu.admin.models;
import com.hello.suripu.core.models.DeviceInactive;
import com.hello.suripu.core.models.DeviceInactivePage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class InactiveDevicesPaginatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InactiveDevicesPaginatorTest.class);
    private static final Long BEFORE_CURSOR = 1418240007000L;
    private static final Long AFTER_CURSOR = 1414956807000L;
    private static final Long MAX_TIMESTAMP = Long.MAX_VALUE;
    private static final Long MIN_TIMESTAMP = Long.MIN_VALUE;
    private static final String DEVICE_TYPE = "test device";
    private static final Integer LIMIT = 10;
    private static final JedisPool jedisPool = mock(JedisPool.class);
    private static final InactiveDevicesPaginator inactiveDevicesPaginator = new InactiveDevicesPaginator(jedisPool, 0L, 1L, DEVICE_TYPE, LIMIT);
    private static final DeviceInactivePage middlePage = simulateMiddlePage();
    private static final DeviceInactivePage firstPage = simulateFirstPage();
    private static final DeviceInactivePage lastPage = simulateLastPage();

    @Test
    public void testPageLimit() {
        final Boolean isPageLimitRespected = middlePage.inactiveDevices.size() <= LIMIT;
        assertThat(isPageLimitRespected, is(true));
    }

    @Test
    public void testPageTimeRange() {
        final Boolean isDeviceTimestampInRange = middlePage.inactiveDevices.get(0).lastSeenTimestamp >= AFTER_CURSOR
                && middlePage.inactiveDevices.get(middlePage.inactiveDevices.size()-1).lastSeenTimestamp <= BEFORE_CURSOR;
        assertThat(isDeviceTimestampInRange, is(true));
    }

    @Test
    public void testMiddlePageNextCursor() {
        final Boolean isNextCursorIncremental = middlePage.nextCursorTimestamp == (middlePage.inactiveDevices.get(middlePage.inactiveDevices.size()-1).lastSeenTimestamp + 1);
        assertThat(isNextCursorIncremental, is(true));
    }

    @Test
    public void testMiddlePagePrevCursor() {
        final Boolean isPrevCursorDecremental = middlePage.previousCursorTimestamp == (middlePage.inactiveDevices.get(0).lastSeenTimestamp - 1);
        assertThat(isPrevCursorDecremental, is(true));
    }

    @Test
    public void testFirstPageNextCursor() {
        final Boolean isNextCursorStationary = firstPage.nextCursorTimestamp == BEFORE_CURSOR;
        assertThat(isNextCursorStationary, is(true));
    }

    @Test
    public void testFirstPagePrevCursor() {
        final Boolean isPrevCursorMin = firstPage.previousCursorTimestamp == MIN_TIMESTAMP;
        assertThat(isPrevCursorMin, is(true));
    }

    @Test
    public void testLastPageNextCursor() {
        final Boolean isNextCursorMax = lastPage.nextCursorTimestamp == MAX_TIMESTAMP;
        assertThat(isNextCursorMax, is(true));
    }

    @Test
    public void testLastPagePrevCursor() {
        final Boolean isPrevCursorStationary = lastPage.previousCursorTimestamp == AFTER_CURSOR;
        assertThat(isPrevCursorStationary, is(true));
    }

    private static DeviceInactivePage simulateMiddlePage() {
        final List<DeviceInactive> inactiveDevices = new ArrayList<>();
        inactiveDevices.add(new DeviceInactive("Device40", 1414956807000L));
        inactiveDevices.add(new DeviceInactive("Device39", 1415043207000L));
        inactiveDevices.add(new DeviceInactive("Device38", 1415129607000L));
        return inactiveDevicesPaginator.formatPage(AFTER_CURSOR, BEFORE_CURSOR, inactiveDevices);
    }

    private static DeviceInactivePage simulateFirstPage() {
        final List<DeviceInactive> inactiveDevices = new ArrayList<>();
        return inactiveDevicesPaginator.formatPage(MIN_TIMESTAMP, BEFORE_CURSOR, inactiveDevices);
    }

    private static DeviceInactivePage simulateLastPage() {
        final List<DeviceInactive> inactiveDevices = new ArrayList<>();
        return inactiveDevicesPaginator.formatPage(AFTER_CURSOR, MAX_TIMESTAMP, inactiveDevices);
    }
}

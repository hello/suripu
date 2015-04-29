package com.hello.suripu.admin.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DeviceInactive;
import com.hello.suripu.core.models.DeviceInactivePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class InactiveDevicesPaginator {
    private static final Logger LOGGER = LoggerFactory.getLogger(InactiveDevicesPaginator.class);
    private static final Integer DEFAULT_MAX_ITEMS_PER_PAGE = 40;

    public final JedisPool jedisPool;
    public final Optional<Long> afterTimestamp;
    public final Optional<Long> beforeTimestamp;
    public final String deviceType;
    public final Optional<Integer> maxItemsPerPageOptional;

    public InactiveDevicesPaginator(final JedisPool jedisPool, final Long afterTimestamp, final Long beforeTimestamp, final String deviceType, final Integer maxItemsPerPage) {
        this.jedisPool = jedisPool;
        this.afterTimestamp = Optional.fromNullable(afterTimestamp);
        this.beforeTimestamp = Optional.fromNullable(beforeTimestamp);
        this.deviceType = deviceType;
        this.maxItemsPerPageOptional = Optional.fromNullable(maxItemsPerPage);
    }

    public InactiveDevicesPaginator(final JedisPool jedisPool, final Long afterTimestamp, final Long beforeTimestamp, final String deviceType) {
        this(jedisPool, afterTimestamp, beforeTimestamp, deviceType, DEFAULT_MAX_ITEMS_PER_PAGE);
    }

    public DeviceInactivePage generatePage() {
        final Jedis jedis = jedisPool.getResource();
        final Set<Tuple> redisSenses = new TreeSet<>();

        // if afterTimestamp is not set, assume minimum timestamp
        final Long afterCursor = (afterTimestamp.isPresent()) ? afterTimestamp.get() : Long.MIN_VALUE;

        // if beforeTimestamp is not set, assume maximum timestamp
        final Long beforeCursor = (beforeTimestamp.isPresent()) ? beforeTimestamp.get() : Long.MAX_VALUE;

        // if no limit is specified, use default limit
        final Integer maxItemsPerPage = (maxItemsPerPageOptional.isPresent()) ? maxItemsPerPageOptional.get() : DEFAULT_MAX_ITEMS_PER_PAGE;

        // Get data from redis
        try {
            redisSenses.addAll(jedis.zrangeByScoreWithScores(deviceType, afterCursor, beforeCursor, 0, maxItemsPerPage));
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve list of devices {}", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }
        final List<DeviceInactive> inactiveDevices = extractDevicesList(redisSenses);
        final DeviceInactivePage formattedPage = internalFormatPage(afterCursor, beforeCursor, inactiveDevices);
        return formattedPage;
    }

    private List<DeviceInactive> extractDevicesList(Set<Tuple> redisSenses) {
        final List<DeviceInactive> inactiveDevices = new ArrayList<>();
        for(final Tuple sense : redisSenses) {
            final Long lastSeenTimestamp = (long) sense.getScore();
            final DeviceInactive inactiveDevice = new DeviceInactive(sense.getElement(), lastSeenTimestamp);
            inactiveDevices.add(inactiveDevice);
        }
        return inactiveDevices;
    }

    private DeviceInactivePage internalFormatPage(final Long afterCursor, final Long beforeCursor, List<DeviceInactive> inactiveDevices) {
        // Prepare page cursors
        Long previousCursorTimestamp = afterCursor;  // Previous page calls "?before=%d" which take the current page's "after"
        Long nextCursorTimestamp = beforeCursor;  // Next page calls "?after=%d" which take the current page's "before"
        if(!inactiveDevices.isEmpty())  {
            previousCursorTimestamp = inactiveDevices.get(0).lastSeenTimestamp - 1;
            nextCursorTimestamp = inactiveDevices.get(inactiveDevices.size() - 1).lastSeenTimestamp + 1;
        }
        final Integer maxItemsPerPage = (maxItemsPerPageOptional.isPresent()) ? maxItemsPerPageOptional.get() : DEFAULT_MAX_ITEMS_PER_PAGE;
        return new DeviceInactivePage(previousCursorTimestamp, nextCursorTimestamp, maxItemsPerPage, inactiveDevices);
    }

    // Just for testing
    public DeviceInactivePage formatPage(final Long afterCursor, final Long beforeCursor, List<DeviceInactive> inactiveDevices) {
        return internalFormatPage(afterCursor, beforeCursor, inactiveDevices);
    }
}

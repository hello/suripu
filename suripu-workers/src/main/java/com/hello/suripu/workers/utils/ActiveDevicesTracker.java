package com.hello.suripu.workers.utils;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.HashMap;
import java.util.Map;

public class ActiveDevicesTracker {

    private final static Logger LOGGER = LoggerFactory.getLogger(ActiveDevicesTracker.class);

    private static final String SENSE_ACTIVE_SET_KEY = "active_senses";
    private static final String PILL_ACTIVE_SET_KEY = "active_pills";

    private final JedisPool jedisPool;

    public ActiveDevicesTracker(final JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void trackSense(final String senseId, final Long lastSeen) {
        final Map<String, Long> seenDevices = new HashMap<>(1);
        seenDevices.put(senseId, lastSeen);
        trackDevices(SENSE_ACTIVE_SET_KEY, ImmutableMap.copyOf(seenDevices));
    }

    public void trackSenses(final Map<String, Long> activeSenses) {
        trackDevices(SENSE_ACTIVE_SET_KEY, ImmutableMap.copyOf(activeSenses));
    }

    public void trackPill(final String pillId, final Long lastSeen) {
        final Map<String, Long> seenDevices = new HashMap<>(1);
        seenDevices.put(pillId, lastSeen);
        trackDevices(PILL_ACTIVE_SET_KEY, ImmutableMap.copyOf(seenDevices));
    }

    public void trackPills(final Map<String, Long> activePills) {
        trackDevices(PILL_ACTIVE_SET_KEY, ImmutableMap.copyOf(activePills));
    }

    private void trackDevices(final String redisKey, final Map<String, Long> devicesSeen) {
        final Jedis jedis = jedisPool.getResource();
        try {
            final Pipeline pipe = jedis.pipelined();
            pipe.multi();
            for(Map.Entry<String, Long> entry : devicesSeen.entrySet()) {
                pipe.zadd(redisKey, entry.getValue(), entry.getKey());
            }
            pipe.exec();
        }catch (JedisDataException exception) {
            LOGGER.error("Failed getting data out of redis: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
            return;
        } catch(Exception exception) {
            LOGGER.error("Unknown error connection to redis: {}", exception.getMessage());
            jedisPool.returnBrokenResource(jedis);
            return;
        }
        finally {
            jedisPool.returnResource(jedis);
        }
        LOGGER.debug("Tracked {} active devices", devicesSeen.size());
    }
}

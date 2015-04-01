package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.FirmwareInfo;
import com.hello.suripu.core.models.FirmwareCountInfo;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

@Path("/v1/firmware")
public class FirmwareResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareResource.class);

    private final DeviceDAO deviceDAO;
    private final JedisPool jedisPool;
    private static final String REDIS_SEEN_FIRMWARE_KEY = "firmwares_seen";

    public FirmwareResource(final DeviceDAO deviceDAO, final JedisPool jedisPool) {
        this.deviceDAO = deviceDAO;
        this.jedisPool = jedisPool;
    }

    @GET
    @Timed
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareInfo> getFirmwareDeviceList(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                              @QueryParam("firmware_version") final Long firmwareVersion,
                                              @QueryParam("range_start") final Long rangeStart,
                                              @QueryParam("range_end") final Long rangeEnd) {
        if(firmwareVersion == null) {
            LOGGER.error("Missing firmwareVersion parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(rangeStart == null) {
            LOGGER.error("Missing range_start parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if(rangeEnd == null) {
            LOGGER.error("Missing range_end parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final String fwVersion = firmwareVersion.toString();
        final List<FirmwareInfo> deviceInfo = new ArrayList<>();
        try {
            //Get all elements in the index range provided
            final Set<Tuple> allFWDevices = jedis.zrevrangeWithScores(fwVersion, rangeStart, rangeEnd);
            for(final Tuple device: allFWDevices){
                deviceInfo.add(new FirmwareInfo(fwVersion, device.getElement(), (long)device.getScore()));
            }
        } catch (Exception e) {
            LOGGER.error("Failed retrieving firmware device list.", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        return deviceInfo;
    }

    @GET
    @Timed
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Long getFirmwareDeviceCount(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                       @QueryParam("firmware_version") final Long firmwareVersion) {
        if(firmwareVersion == null) {
            LOGGER.error("Missing firmwareVersion parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final String fwVersion = firmwareVersion.toString();
        Long devicesOnFirmware = 0L;

        try {
            devicesOnFirmware = jedis.zcard(fwVersion);

        } catch (Exception e) {
            LOGGER.error("Failed retrieving firmware device count.", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        return devicesOnFirmware;
    }

    @GET
    @Timed
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareCountInfo> getAllSeenFirmwares(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken) {

        final Jedis jedis = jedisPool.getResource();
        final List<FirmwareCountInfo> firmwareCounts = new ArrayList<>();
        try {
            final Set<String> seenFirmwares = jedis.smembers(REDIS_SEEN_FIRMWARE_KEY);
            for (String fw_version:seenFirmwares) {
                final long fwCount = jedis.zcard(fw_version);
                if (fwCount > 0) {
                    final long lastSeen = (long)jedis.zrevrangeWithScores(fw_version, 0, 1).iterator().next().getScore();
                    firmwareCounts.add(new FirmwareCountInfo(fw_version, fwCount, lastSeen));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed retrieving all seen firmwares.", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        return firmwareCounts;
    }

    @GET
    @Timed
    @Path("/{device_id}/history")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Long, String> getFirmwareHistory(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                 @PathParam("device_id") final String deviceId) {

        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final TreeMap<Long, String> fwHistory = new TreeMap<>();

        try {
            final Set<String> seenFirmwares = jedis.smembers(REDIS_SEEN_FIRMWARE_KEY);
            for (String fw_version:seenFirmwares) {
                final Double score = jedis.zscore(fw_version, deviceId);
                if(score != null) {
                    fwHistory.put(score.longValue(), fw_version);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed retrieving fw history for device.", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        return fwHistory;
    }

    @DELETE
    @Timed
    @Path("/history/{fw_version}/")
    public void clearFWHistory(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                      @PathParam("fw_version") final String fwVersion) {
        if(fwVersion == null) {
            LOGGER.error("Missing fw_version parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        try {
            if (jedis.srem(REDIS_SEEN_FIRMWARE_KEY, fwVersion) > 0) {
                jedis.del(fwVersion);
            } else {
                LOGGER.error("Attempted to delete non-existent Redis member: {}", fwVersion);
            }
        } catch (Exception e) {
            LOGGER.error("Failed clearing fw history for {} {}.", fwVersion, e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

    }
}

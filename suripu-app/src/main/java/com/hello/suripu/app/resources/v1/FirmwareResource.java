package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.FirmwareInfo;
import com.hello.suripu.core.models.FirmwareCountInfo;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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

    public FirmwareResource(final DeviceDAO deviceDAO, final JedisPool jedisPool) {
        this.deviceDAO = deviceDAO;
        this.jedisPool = jedisPool;
    }

    @GET
    @Timed
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public LinkedList<FirmwareInfo> getFirmwareDeviceList(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                              @QueryParam("firmware_version") final Long firmwareVersion) {
        if(firmwareVersion == null) {
            LOGGER.error("Missing firmwareVersion parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final String fwVersion = firmwareVersion.toString();
        LinkedList<FirmwareInfo> deviceInfo = new LinkedList<>();
        try {

            final Set<Tuple> allFWDevices = jedis.zrevrangeWithScores(fwVersion, 0, -1);
            for(Tuple device: allFWDevices){
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
        List<FirmwareCountInfo> firmwareCounts = new LinkedList<>();
        try {
            final Set<String> seenFirmwares = jedis.smembers("firmwares_seen");
            for (String fw_version:seenFirmwares) {
                final long fwCount = jedis.zcard(fw_version);
                firmwareCounts.add(new FirmwareCountInfo(fw_version, fwCount));
            }
        } catch (Exception e) {
            LOGGER.error("Failed retrieving all seen firmwares.", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        return firmwareCounts;
    }
}

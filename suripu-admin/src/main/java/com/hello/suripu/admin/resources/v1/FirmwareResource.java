package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.FirmwareVersionMappingDAO;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB.ResponseCommand;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.FirmwareCountInfo;
import com.hello.suripu.core.models.FirmwareInfo;
import com.hello.suripu.core.models.OTAHistory;
import com.hello.suripu.core.models.Team;
import com.hello.suripu.core.models.UpgradeNodeRequest;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Tuple;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Path("/v1/firmware")
public class FirmwareResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareResource.class);

    private final FirmwareVersionMappingDAO firmwareVersionMappingDAO;
    final FirmwareUpgradePathDAO firmwareUpgradePathDAO;
    private final OTAHistoryDAODynamoDB otaHistoryDAO;
    private final ResponseCommandsDAODynamoDB responseCommandsDAODynamoDB;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final TeamStore teamStore;
    private final DeviceDAO deviceDAO;
    private final JedisPool jedisPool;
    private static final String REDIS_SEEN_FIRMWARE_KEY = "firmwares_seen";

    public FirmwareResource(final JedisPool jedisPool,
                            final FirmwareVersionMappingDAO firmwareVersionMappingDAO,
                            final OTAHistoryDAODynamoDB otaHistoryDAODynamoDB,
                            final ResponseCommandsDAODynamoDB responseCommandsDAODynamoDB,
                            final FirmwareUpgradePathDAO firmwareUpgradePathDAO,
                            final DeviceDAO deviceDAO,
                            final SensorsViewsDynamoDB sensorsViewsDynamoDB,
                            final TeamStore teamStore) {
        this.jedisPool = jedisPool;
        this.firmwareVersionMappingDAO = firmwareVersionMappingDAO;
        this.otaHistoryDAO = otaHistoryDAODynamoDB;
        this.responseCommandsDAODynamoDB = responseCommandsDAODynamoDB;
        this.firmwareUpgradePathDAO = firmwareUpgradePathDAO;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.deviceDAO = deviceDAO;
        this.teamStore = teamStore;
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
        final List<FirmwareInfo> deviceInfo = Lists.newArrayList();
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
        final List<FirmwareCountInfo> firmwareCounts = Lists.newArrayList();
        try {
            final Set<Tuple> seenFirmwares = jedis.zrangeWithScores(REDIS_SEEN_FIRMWARE_KEY, 0, -1);
            final Pipeline pipe = jedis.pipelined();
            final Map<String, redis.clients.jedis.Response<Long>> responseMap = Maps.newHashMap();
            for (final Tuple fwInfo:seenFirmwares) {
                responseMap.put(fwInfo.getElement(), pipe.zcard(fwInfo.getElement()));
            }
            pipe.sync();
            for (final Tuple fwInfo:seenFirmwares) {
                final String fwVersion = fwInfo.getElement();
                final long fwCount = responseMap.get(fwVersion).get();
                final long lastSeen = (long) fwInfo.getScore();
                if (fwCount > 0) {
                    firmwareCounts.add(new FirmwareCountInfo(fwInfo.getElement(), fwCount, lastSeen));
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
    @Path("/list_by_time")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareCountInfo> getSeenFirmwareByTime(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                            @QueryParam("range_start") final Long rangeStart,
                                                            @QueryParam("range_end") final Long rangeEnd) {

        if(rangeStart == null) {
            LOGGER.error("Missing range_start parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if(rangeEnd == null) {
            LOGGER.error("Missing range_end parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final List<FirmwareCountInfo> firmwareCounts = Lists.newArrayList();
        try {
            final Set<Tuple> seenFirmwares = jedis.zrangeByScoreWithScores(REDIS_SEEN_FIRMWARE_KEY, rangeStart, rangeEnd);
            final Pipeline pipe = jedis.pipelined();
            final Map<String, redis.clients.jedis.Response<Long>> responseMap = Maps.newHashMap();
            for (final Tuple fwInfo:seenFirmwares) {
                responseMap.put(fwInfo.getElement(), pipe.zcount(fwInfo.getElement(), rangeStart, rangeEnd));
            }
            pipe.sync();
            for (final Tuple fwInfo:seenFirmwares) {
                final String fwVersion = fwInfo.getElement();
                final long fwCount = responseMap.get(fwVersion).get();
                final long lastSeen = (long) fwInfo.getScore();
                if (fwCount > 0) {
                    firmwareCounts.add(new FirmwareCountInfo(fwInfo.getElement(), fwCount, lastSeen));
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed retrieving all seen firmware by time.", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        return firmwareCounts;
    }

    @GET
    @Timed
    @Path("/{device_id}/history")
    @Produces(MediaType.APPLICATION_JSON)
    public TreeMap<Long, String> getFirmwareHistory(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                 @PathParam("device_id") final String deviceId) {

        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Jedis jedis = jedisPool.getResource();
        final TreeMap<Long, String> fwHistory = Maps.newTreeMap();

        try {
            final Set<Tuple> seenFirmwares = jedis.zrangeWithScores(REDIS_SEEN_FIRMWARE_KEY, 0, -1);
            for (final Tuple fwInfo:seenFirmwares) {
                final String fwVersion = fwInfo.getElement();
                final Double score = jedis.zscore(fwVersion, deviceId);
                if(score != null) {
                    fwHistory.put(score.longValue(), fwVersion);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed retrieving fw history for device.", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        return fwHistory;
    }

    @GET
    @Timed
    @Path("/{device_id}/latest")
    @Produces(MediaType.APPLICATION_JSON)
    public FirmwareInfo getLatestFirmwareVersion(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                @PathParam("device_id") final String deviceId) {

        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Optional<FirmwareInfo> latestInfo = getFirmwareVersionForDevice(deviceId);
        if (!latestInfo.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return latestInfo.get();
    }

    @GET
    @Timed
    @Path("/{group_name}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FirmwareInfo> getFirmwareStatusForGroup(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                            @PathParam("group_name") final String groupName) {

        if(groupName == null) {
            LOGGER.error("Missing groupName parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        //Get list of all devices in group
        final Optional<Team> team = teamStore.getTeam(groupName, TeamStore.Type.DEVICES);
        if(!team.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final Team group = team.get();
        final List<String> groupIds = Lists.newArrayList(group.ids);
        final List<List<String>> devices = Lists.partition(groupIds, SensorsViewsDynamoDB.MAX_LAST_SEEN_DEVICES);

        final List<FirmwareInfo> firmwares = Lists.newArrayList();
        for (final List<String> deviceSubList : devices) {
            final Optional<List<FirmwareInfo>> fwInfo = sensorsViewsDynamoDB.lastSeenFirmwareBatch(Sets.newHashSet(deviceSubList));
            if (fwInfo.isPresent()) {
                firmwares.addAll(fwInfo.get());
            }
        }
        return firmwares;
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
            if (jedis.zrem(REDIS_SEEN_FIRMWARE_KEY, fwVersion) > 0) {
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


    @GET
    @Timed
    @Path("/names/{fw_hash}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getFWNames(
            @Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
            @PathParam("fw_hash") final String fwHash) {
        return firmwareVersionMappingDAO.get(fwHash);
    }

    @POST
    @Timed
    @Path("/names")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> getFWNamesBatch(
            @Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
            @Valid @NotNull final ImmutableSet<String> fwHashSet) {
        return firmwareVersionMappingDAO.getBatch(fwHashSet);
    }

    @GET
    @Timed
    @Path("/{device_id}/ota_history")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OTAHistory> getOTAHistory(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                           @PathParam("device_id") final String deviceId,
                                           @QueryParam("range_start") final Long rangeStart,
                                           @QueryParam("range_end") final Long rangeEnd) {

        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
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

        final DateTime rangeStartDate = new DateTime(rangeStart * 1000);
        final DateTime rangeEndDate = new DateTime(rangeEnd * 1000);

        final List<OTAHistory> otaHistoryEntries = otaHistoryDAO.getOTAEvents(deviceId, rangeStartDate, rangeEndDate);

        return otaHistoryEntries;
    }

    @PUT
    @Timed
    @Path("/{device_id}/reset_to_factory_fw")
    public void resetDeviceToFactoryFW(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                                    @PathParam("device_id") final String deviceId,
                                                    @QueryParam("fw_version") final Integer fwVersion) {
        if(deviceId == null) {
            LOGGER.error("Missing device_id parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(fwVersion == null) {
            LOGGER.error("Missing fw_version parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        LOGGER.info("Resetting to factory FW for device: {} on FW Version: {}", deviceId, fwVersion);
        final Map<ResponseCommand, String> issuedCommands = new HashMap<>();
        issuedCommands.put(ResponseCommand.RESET_TO_FACTORY_FW, "true");
        responseCommandsDAODynamoDB.insertResponseCommands(deviceId, fwVersion, issuedCommands);
    }

    @GET
    @Timed
    @Path("/{group_name}/upgrade_nodes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UpgradeNodeRequest> getFWUpgradeNode(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken, @PathParam("group_name") final String groupName) {

        if(groupName == null) {
            LOGGER.error("Missing group name parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        LOGGER.info("Retrieving FW upgrade node(s) for group: {}", groupName);

        return firmwareUpgradePathDAO.getFWUpgradeNodesForGroup(groupName);
    }

    @PUT
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/upgrades/add_node")
    public void addFWUpgradeNode(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final UpgradeNodeRequest nodeRequest) {

        LOGGER.info("Adding FW upgrade node for group: {} on FW Version: {} to FW Version: {} @ {}% Rollout", nodeRequest.groupName, nodeRequest.fromFWVersion, nodeRequest.toFWVersion, nodeRequest.rolloutPercent);
        firmwareUpgradePathDAO.insertFWUpgradeNode(nodeRequest);
    }

    @DELETE
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/upgrades/delete_node/{group_name}/{from_fw_version}")
    public void deleteFWUpgradeNode(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                    @PathParam("group_name") final String groupName,
                                    @PathParam("from_fw_version") final Integer fromFWVersion) {

        LOGGER.info("Deleting FW upgrade node for group: {} on FW Version: {}", groupName, fromFWVersion);
        firmwareUpgradePathDAO.deleteFWUpgradeNode(groupName, fromFWVersion);
    }

    private Optional<FirmwareInfo> getFirmwareVersionForDevice(final String deviceId) {
        final List<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(deviceId);
        if(pairs.isEmpty()) {
           return Optional.absent();
        }

        final DeviceAccountPair pair = pairs.get(0);

        final Optional<DeviceData> deviceDataOptional = sensorsViewsDynamoDB.lastSeen(deviceId, pair.accountId, pair.internalDeviceId);
        if(!deviceDataOptional.isPresent()) {
            return Optional.absent();
        }
        final FirmwareInfo fwInfo = new FirmwareInfo(deviceDataOptional.get().firmwareVersion.toString(), deviceId, deviceDataOptional.get().dateTimeUTC.getMillis());
        return Optional.of(fwInfo);
    }

}
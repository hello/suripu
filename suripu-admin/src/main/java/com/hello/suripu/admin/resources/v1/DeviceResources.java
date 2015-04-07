package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.models.DeviceAdmin;
import com.hello.suripu.admin.models.DeviceStatusBreakdown;
import com.hello.suripu.admin.models.InactiveDevicesPaginator;
import com.hello.suripu.core.configuration.ActiveDevicesTrackerConfiguration;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDAOAdmin;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceInactivePage;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/v1/devices")
public class DeviceResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);
    private static final Integer SENSE_STATUS_WAITING_HOURS = 3;
    private static final Integer PILL_STATUS_WAITING_HOURS = 3;

    private final DeviceDAO deviceDAO;
    private final DeviceDAOAdmin deviceDAOAdmin;
    private final DeviceDataDAO deviceDataDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AccountDAO accountDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final KeyStore senseKeyStore;
    private final KeyStore pillKeyStore;
    private final JedisPool jedisPool;

    public DeviceResources(final DeviceDAO deviceDAO,
                           final DeviceDAOAdmin deviceDAOAdmin,
                           final DeviceDataDAO deviceDataDAO,
                           final TrackerMotionDAO trackerMotionDAO,
                           final AccountDAO accountDAO,
                           final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                           final KeyStore senseKeyStore,
                           final KeyStore pillKeyStore,
                           final JedisPool jedisPool) {
        this.deviceDAO = deviceDAO;
        this.deviceDAOAdmin = deviceDAOAdmin;
        this.accountDAO = accountDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.senseKeyStore = senseKeyStore;
        this.pillKeyStore = pillKeyStore;
        this.deviceDataDAO = deviceDataDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.jedisPool = jedisPool;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sense")
    public List<DeviceAdmin> getSensesByEmail(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                        @QueryParam("email") final String email) {
        LOGGER.debug("Querying all senses for email = {}", email);
        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("Account not found!").build());
        }
        return getSensesByAccountId(accountIdOptional.get());
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pill")
    public List<DeviceAdmin> getPillsByEmail(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                       @QueryParam("email") final String email) {
        LOGGER.debug("Querying all pills for email = {}", email);
        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("Account not found!").build());
        }
        return getPillsByAccountId(accountIdOptional.get());
    }

    @GET
    @Timed
    @Path("/pill_status")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceStatus> getPillStatus(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                            @QueryParam("email") final String email,
                                            @QueryParam("pill_id_partial") final String pillIdPartial,
                                            @QueryParam("end_ts") final Long endTs) {

        final List<DeviceAccountPair> pills = new ArrayList<>();
        if (email == null && pillIdPartial == null){
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing query params!").build());
        }
        else if (email != null) {
            LOGGER.debug("Querying all pills for email = {}", email);
            final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
            if (!accountIdOptional.isPresent()) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity("Account not found!").build());
            }
            pills.addAll(deviceDAO.getPillsForAccountId(accountIdOptional.get()));
        }

        else {
            LOGGER.debug("Querying all pills whose IDs contain = {}", pillIdPartial);
            pills.addAll(deviceDAOAdmin.getPillsByPillIdHint(pillIdPartial));
        }

        final List<DeviceStatus> pillStatuses = new ArrayList<>();
        for (final DeviceAccountPair pill : pills) {
            pillStatuses.addAll(deviceDAOAdmin.pillStatusBeforeTs(pill.internalDeviceId, new DateTime(endTs, DateTimeZone.UTC)));
        }

        return pillStatuses;
    }

    @Timed
    @GET
    @Path("/{device_id}/accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<Account> getAccountsByDeviceIDs(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                         @QueryParam("max_devices") final Long maxDevices,
                                                         @PathParam("device_id") final String deviceId) {
        final List<Account> accounts = new ArrayList<>();
        LOGGER.debug("Searching accounts who have used device {}", deviceId);
        accounts.addAll(deviceDAOAdmin.getAccountsBySenseId(deviceId, maxDevices));
        if (accounts.isEmpty()) {
            accounts.addAll(deviceDAOAdmin.getAccountsByPillId(deviceId, maxDevices));
        }
        return ImmutableList.copyOf(accounts);
    }

    @Timed
    @GET
    @Path("/status_breakdown")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceStatusBreakdown getDeviceStatusBreakdown(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                          @QueryParam("start_ts") final Long startTs,
                                                          @QueryParam("end_ts") final Long endTs) {
        // TODO: move this out of url handler once we've validated this is what we want

        if (startTs == null || endTs == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Require start_ts & end_ts").build());
        }

        final Jedis jedis = jedisPool.getResource();
        Long sensesCount = -1L;
        Long pillsCount = -1L;

        try {
            sensesCount = jedis.zcount(ActiveDevicesTrackerConfiguration.SENSE_ACTIVE_SET_KEY, startTs, endTs);
            pillsCount = jedis.zcount(ActiveDevicesTrackerConfiguration.PILL_ACTIVE_SET_KEY, startTs, endTs);
        } catch (Exception e) {
            LOGGER.error("Failed to get active senses count because {}", e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build());
        } finally {
            jedisPool.returnResource(jedis);
        }

        LOGGER.debug("Senses count is {} from {} to {}", sensesCount, startTs, endTs);
        LOGGER.debug("Pills count is {} from {} to {}", pillsCount, startTs, endTs);

        return new DeviceStatusBreakdown(sensesCount, pillsCount);
    }

    @GET
    @Timed
    @Path("/inactive/sense")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceInactivePage getInactiveSenses(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                @QueryParam("after") final Long afterTimestamp,
                                                @QueryParam("before") final Long beforeTimestamp) {

        final InactiveDevicesPaginator redisPaginator = new InactiveDevicesPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.SENSE_ACTIVE_SET_KEY);
        final DeviceInactivePage inactiveSensesPage = redisPaginator.generatePage();
        return inactiveSensesPage;
    }

    @GET
    @Timed
    @Path("/inactive/pill")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceInactivePage getInactivePills(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                               @QueryParam("after") final Long afterTimestamp,
                                               @QueryParam("before") final Long beforeTimestamp) {

        final InactiveDevicesPaginator inactiveDevicesPaginator = new InactiveDevicesPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.PILL_ACTIVE_SET_KEY);
        final DeviceInactivePage inactivePillsPage = inactiveDevicesPaginator.generatePage();
        return inactivePillsPage;
    }

    @GET
    @Timed
    @Path("/key_store_hints/sense/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceKeyStoreRecord getKeyHintForSense(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                   @PathParam("sense_id") final String senseId) {
        final Optional<DeviceKeyStoreRecord> senseKeyStoreRecord = senseKeyStore.getKeyStoreRecord(senseId);
        if (!senseKeyStoreRecord.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("This sense has not been properly provisioned!").build());
        }
        return senseKeyStoreRecord.get();
    }

    @GET
    @Timed
    @Path("/key_store_hints/pill/{pill_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceKeyStoreRecord getKeyHintForPill(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                  @PathParam("pill_id") final String pillId) {
        final Optional<DeviceKeyStoreRecord> pillKeyStoreRecord = pillKeyStore.getKeyStoreRecord(pillId);
        if (!pillKeyStoreRecord.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("This pill has not been properly provisioned!").build());
        }
        return pillKeyStoreRecord.get();
    }

    // Helpers
    private List<DeviceAdmin> getSensesByAccountId(final Long accountId) {
        final ImmutableList<DeviceAccountPair> senseAccountPairs = deviceDAO.getSensesForAccountId(accountId);
        final List<DeviceAdmin> senses = new ArrayList<>();

        for (final DeviceAccountPair senseAccountPair: senseAccountPairs) {
            final Optional<DeviceStatus> senseStatusOptional = this.deviceDataDAO.senseStatus(senseAccountPair.internalDeviceId);
            senses.add(new DeviceAdmin(senseAccountPair, senseStatusOptional.orNull()));
        }
        return senses;
    }

    private List<DeviceAdmin> getPillsByAccountId(final Long accountId) {
        final ImmutableList<DeviceAccountPair> pillAccountPairs = deviceDAO.getPillsForAccountId(accountId);
        final List<DeviceAdmin> pills = new ArrayList<>();

        for (final DeviceAccountPair pillAccountPair: pillAccountPairs) {
            final Optional<DeviceStatus> pillStatusOptional = this.trackerMotionDAO.pillStatus(pillAccountPair.internalDeviceId);
            pills.add(new DeviceAdmin(pillAccountPair, pillStatusOptional.orNull()));
        }
        return pills;
    }


}

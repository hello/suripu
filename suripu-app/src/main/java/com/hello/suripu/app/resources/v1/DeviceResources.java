package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceInactive;
import com.hello.suripu.core.models.DeviceInactivePaginator;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.DeviceInactivePage;
import com.hello.suripu.core.models.PillRegistration;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

@Path("/v1/devices")
public class DeviceResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);

    private final DeviceDAO deviceDAO;
    private final AccountDAO accountDAO;
    private final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB;
    private final JedisPool jedisPool;

    public DeviceResources(final DeviceDAO deviceDAO,
                           final AccountDAO accountDAO,
                           final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB,
                           final JedisPool jedisPool) {
        this.deviceDAO = deviceDAO;
        this.accountDAO = accountDAO;
        this.jedisPool = jedisPool;
        this.mergedAlarmInfoDynamoDB = mergedAlarmInfoDynamoDB;
    }

    @POST
    @Path("/pill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void registerPill(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final PillRegistration pillRegistration) {
        try {
            final Long trackerId = deviceDAO.registerTracker(accessToken.accountId, pillRegistration.pillId);
            LOGGER.info("Account {} registered pill {} with internal id = {}", accessToken.accountId, pillRegistration.pillId, trackerId);
            return;
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());

            if(matcher.find()) {
                LOGGER.error("Failed to register pill for account id = {} and pill id = {} : {}", accessToken.accountId, pillRegistration.pillId, exception.getMessage());
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(new JsonError(409, "Pill already exists for this account.")).build());
            }
        }

        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public List<Device> getDevices(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {
        return getDevicesByAccountId(accessToken.accountId);
    }

    @DELETE
    @Timed
    @Path("/pill/{pill_id}")
    public void unregisterPill(@Scope(OAuthScope.DEVICE_INFORMATION_WRITE) final AccessToken accessToken,
                               @PathParam("pill_id") String pillId) {
        final Integer numRows = deviceDAO.unregisterTracker(pillId);
        if(numRows == 0) {
            LOGGER.warn("Did not find active pill to unregister");
        }
    }

    @DELETE
    @Timed
    @Path("/sense/{sense_id}")
    public void unregisterSense(@Scope(OAuthScope.DEVICE_INFORMATION_WRITE) final AccessToken accessToken,
                               @PathParam("sense_id") String senseId) {
        final Integer numRows = deviceDAO.unregisterSense(senseId);
        final Optional<AlarmInfo> alarmInfoOptional = this.mergedAlarmInfoDynamoDB.unlinkAccountToDevice(accessToken.accountId, senseId);

        // WARNING: Shall we throw error if the dynamoDB unlink fail?
        if(numRows == 0) {
            LOGGER.warn("Did not find active sense to unregister");
        }

        if(!alarmInfoOptional.isPresent()){
            LOGGER.warn("Cannot find device {} account {} pair in merge info table.", senseId, accessToken.accountId);
        }
    }

    @Timed
    @GET
    @Path("/q")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listDeviceIDsByEmail(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                          @QueryParam("email") String email) {
        LOGGER.debug("Searching devices for email = {}", email);
        Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final List<DeviceAccountPair> devices = deviceDAO.getDeviceAccountMapFromAccountId(accountId.get());

        final List<String> deviceIdList = new ArrayList<>();
        for (DeviceAccountPair pair: devices) {
            deviceIdList.add(pair.externalDeviceId);
        }
        return deviceIdList;
    }

    @GET
    @Timed
    @Path("/specs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Device> getDevicesForAdmin(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                           @QueryParam("email") String email) {
        LOGGER.debug("Querying latest devices specs for email = {}", email);
        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return getDevicesByAccountId(accountId.get());
    }

    private Optional<Long> getAccountIdByEmail(final String email) {
        final Optional<Account> accountOptional = accountDAO.getByEmail(email);

        if (!accountOptional.isPresent()) {
            LOGGER.debug("Account {} not found", email);
            return Optional.absent();
        }
        final Account account = accountOptional.get();
        if (!account.id.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            return Optional.absent();
        }
        return account.id;
    }

    private List<Device> getDevicesByAccountId(final Long accountId) {
        // TODO: make asynchronous calls to grab Pills + Senses if the following is too slow
        final ImmutableList<DeviceAccountPair> senses = deviceDAO.getSensesForAccountId(accountId);
        final ImmutableList<DeviceAccountPair> pills = deviceDAO.getPillsForAccountId(accountId);
        final List<Device> devices = new ArrayList<Device>();

        // TODO: device state will always be normal for now until more information is provided by the device
        for (final DeviceAccountPair sense : senses) {
            final Optional<DeviceStatus> senseStatusOptional = deviceDAO.senseStatus(sense.internalDeviceId);
            if(senseStatusOptional.isPresent()) {
                devices.add(new Device(Device.Type.SENSE, sense.externalDeviceId, Device.State.NORMAL, senseStatusOptional.get().firmwareVersion, senseStatusOptional.get().lastSeen));
            } else {
                devices.add(new Device(Device.Type.SENSE, sense.externalDeviceId, Device.State.UNKNOWN, "-", new DateTime(1970,1,1, 0,0,0)));
            }
        }

        for (final DeviceAccountPair pill : pills) {
            final Optional<DeviceStatus> pillStatusOptional = deviceDAO.pillStatus(pill.internalDeviceId);
            if(!pillStatusOptional.isPresent()) {
                LOGGER.debug("No pill status found for pill_id = {} ({}) for account: {}", pill.externalDeviceId, pill.internalDeviceId, pill.accountId);
                devices.add(new Device(Device.Type.PILL, pill.externalDeviceId, Device.State.UNKNOWN, null, null));
            } else {
                final DeviceStatus deviceStatus = pillStatusOptional.get();
                devices.add(new Device(Device.Type.PILL, pill.externalDeviceId, Device.State.NORMAL, deviceStatus.firmwareVersion, deviceStatus.lastSeen));
            }
        }

        return devices;
    }


    @GET
    @Timed
    @Path("/inactive")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceInactivePaginator getInactiveDevices(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                   @QueryParam("start") final Long startTimeStamp,
                                                   @QueryParam("since") final Long inactiveSince,
                                                   @QueryParam("threshold") final Long inactiveThreshold,
                                                   @QueryParam("page") final Integer currentPage) {
        if(startTimeStamp == null) {
            LOGGER.error("Missing startTimestamp parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(inactiveSince == null) {
            LOGGER.error("Missing inactiveSince parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(inactiveThreshold == null) {
            LOGGER.error("Missing inactiveThreshold parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(currentPage == null) {
            LOGGER.error("Missing page parameter");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Integer maxDevicesPerPage = 40;
        final Integer offset = Math.max(0, (currentPage - 1) * maxDevicesPerPage);
        final Integer count = maxDevicesPerPage;

        final Jedis jedis = jedisPool.getResource();
        final Set<Tuple> tuples = new TreeSet<>();
        Integer totalPages = 0;

        LOGGER.debug("{} {} {} {}", inactiveSince - inactiveThreshold, inactiveSince, offset, count);
        try {
          // e.g for startTimeStamp = 1417464883000: only care about devices last seen after Dec 1, 2014
          // for inactiveSince = 1418070001000 e.g for inactiveThreshold = 259200000, this function returns
          // devices which have been inactive for at least 3 days since Dec 4, 2014
            tuples.addAll(jedis.zrangeByScoreWithScores("devices", startTimeStamp, inactiveSince - inactiveThreshold, offset, count));
            totalPages = (int)Math.ceil(jedis.zcount("devices", startTimeStamp, inactiveSince - inactiveThreshold) / (double) maxDevicesPerPage);

        } catch (Exception e) {
            LOGGER.error("Failed retrieving list of devices", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        final List<DeviceInactive> inactiveDevices = new ArrayList<>();
        for(final Tuple tuple : tuples) {
            final Long lastSeenTimestamp = (long) tuple.getScore();
            final Long inactivePeriod = inactiveSince - lastSeenTimestamp;
            final DeviceInactive deviceInactive = new DeviceInactive(tuple.getElement(), inactivePeriod);
            inactiveDevices.add(deviceInactive);
        }
        return new DeviceInactivePaginator(currentPage, totalPages, inactiveDevices);
    }

    @Timed
    @GET
    @Path("/{device_id}/accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public ImmutableList<Account> getAccountsByDeviceIDs(@Scope(OAuthScope.ADMINISTRATION_READ) AccessToken accessToken,
                                                @QueryParam("max_devices") Long maxDevices,
                                                @PathParam("device_id") String deviceId) {
        LOGGER.debug("Searching accounts who have used device {}", deviceId);
        final ImmutableList<Account> accounts = deviceDAO.getAccountsByDevices(deviceId, maxDevices);
        return accounts;
    }

    @GET
    @Timed
    @Path("/inactive/sense")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceInactivePage getInactiveSenses(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                    @QueryParam("after") Long afterTimestamp,
                                                    @QueryParam("before") Long beforeTimestamp) {

        final DeviceInactivePage inactiveSensesPage = paginateInactiveDevices(afterTimestamp, beforeTimestamp, "active_senses");
        return inactiveSensesPage;
    }

    @GET
    @Timed
    @Path("/inactive/pill")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceInactivePage getInactivePills(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                @QueryParam("after") Long afterTimestamp,
                                                @QueryParam("before") Long beforeTimestamp) {

        final DeviceInactivePage inactivePillsPage = paginateInactiveDevices(afterTimestamp, beforeTimestamp, "active_pills");
        return inactivePillsPage;
    }

    private DeviceInactivePage paginateInactiveDevices(Long afterTimestamp, Long beforeTimestamp, String deviceType) {
        final List<DeviceInactive> inactiveDevices = new ArrayList<>();
        final Jedis jedis = jedisPool.getResource();
        final Set<Tuple> redisSenses = new TreeSet<>();
        final Integer maxItemsPerPage = 40;

        if(afterTimestamp == null) {
            afterTimestamp = Long.MIN_VALUE;
        }
        if(beforeTimestamp == null) {
            beforeTimestamp = Long.MAX_VALUE;
        }

        try {
            redisSenses.addAll(jedis.zrangeByScoreWithScores(deviceType, afterTimestamp, beforeTimestamp, 0, maxItemsPerPage));
        } catch (Exception e) {
            LOGGER.error("Failed retrieving list of devices", e.getMessage());
        } finally {
            jedisPool.returnResource(jedis);
        }

        for(final Tuple sense : redisSenses) {
            final Long lastSeenTimestamp = (long) sense.getScore();
            final Long inactivePeriod = beforeTimestamp - lastSeenTimestamp;
            final DeviceInactive inactiveSense = new DeviceInactive(sense.getElement(), inactivePeriod);
            inactiveDevices.add(inactiveSense);
        }
        return DeviceInactivePage.getInactivePageByRawInput(inactiveDevices, afterTimestamp, beforeTimestamp, maxItemsPerPage);
    }
}

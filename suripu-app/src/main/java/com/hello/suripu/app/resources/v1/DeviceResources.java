package com.hello.suripu.app.resources.v1;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.app.models.RedisPaginator;
import com.hello.suripu.core.configuration.ActiveDevicesTrackerConfiguration;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceInactive;
import com.hello.suripu.core.models.DeviceInactivePage;
import com.hello.suripu.core.models.DeviceInactivePaginator;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.PairingInfo;
import com.hello.suripu.core.models.PillRegistration;
import com.hello.suripu.core.models.SenseRegistration;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.PillColorUtil;
import com.yammer.metrics.annotation.Timed;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

@Path("/v1/devices")
public class DeviceResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);

    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AccountDAO accountDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final JedisPool jedisPool;
    private final KeyStore senseKeyStore;
    private final KeyStore pillKeyStore;

    public DeviceResources(final DeviceDAO deviceDAO,
                           final DeviceDataDAO deviceDataDAO,
                           final TrackerMotionDAO trackerMotionDAO,
                           final AccountDAO accountDAO,
                           final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                           final JedisPool jedisPool,
                           final KeyStore senseKeyStore,
                           final KeyStore pillKeyStore) {
        this.deviceDAO = deviceDAO;
        this.accountDAO = accountDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.jedisPool = jedisPool;
        this.senseKeyStore = senseKeyStore;
        this.pillKeyStore = pillKeyStore;
        this.deviceDataDAO = deviceDataDAO;
        this.trackerMotionDAO = trackerMotionDAO;
    }

    @GET
    @Timed
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public PairingInfo getPairedSensesByAccount(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {
        final Optional<DeviceAccountPair> optionalPair = deviceDAO.getMostRecentSensePairByAccountId(accessToken.accountId);
        if(!optionalPair.isPresent()) {
            LOGGER.warn("No sense paired for account = {}", accessToken.accountId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final DeviceAccountPair pair = optionalPair.get();
        final ImmutableList<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(pair.externalDeviceId);
        return PairingInfo.create(pair.externalDeviceId, pairs.size());
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
                               @PathParam("pill_id") String externalPillId) {
        final Integer numRows = deviceDAO.deletePillPairing(externalPillId, accessToken.accountId);
        if(numRows == 0) {
            LOGGER.warn("Did not find active pill to unregister");
        }

        final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accessToken.accountId);
        if(sensePairedWithAccount.size() == 0){
            LOGGER.error("No sense paired with account {}", accessToken.accountId);
            return;
        }

        final String senseId = sensePairedWithAccount.get(0).externalDeviceId;

        try {
            this.mergedUserInfoDynamoDB.deletePillColor(senseId, accessToken.accountId, externalPillId);
        }catch (Exception ex){
            LOGGER.error("Delete pill {}'s color from user info table for sense {} and account {} failed: {}",
                    externalPillId,
                    senseId,
                    accessToken.accountId,
                    ex.getMessage());
        }
    }

    @DELETE
    @Timed
    @Path("/sense/{sense_id}")
    public void unregisterSense(@Scope(OAuthScope.DEVICE_INFORMATION_WRITE) final AccessToken accessToken,
                               @PathParam("sense_id") String senseId) {
        final Integer numRows = deviceDAO.deleteSensePairing(senseId, accessToken.accountId);
        final Optional<UserInfo> alarmInfoOptional = this.mergedUserInfoDynamoDB.unlinkAccountToDevice(accessToken.accountId, senseId);

        // WARNING: Shall we throw error if the dynamoDB unlink fail?
        if(numRows == 0) {
            LOGGER.warn("Did not find active sense to unregister");
        }

        if(!alarmInfoOptional.isPresent()){
            LOGGER.warn("Cannot find device {} account {} pair in merge info table.", senseId, accessToken.accountId);
        }
    }

    @DELETE
    @Timed
    @Path("/sense/{sense_id}/all")
    public void unregisterSenseByUser(@Scope(OAuthScope.DEVICE_INFORMATION_WRITE) final AccessToken accessToken,
                                      @PathParam("sense_id") final String senseId) {
        final List<UserInfo> pairedUsers = mergedUserInfoDynamoDB.getInfo(senseId);

        try {
            this.deviceDAO.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new Transaction<Void, DeviceDAO>() {
                @Override
                public Void inTransaction(final DeviceDAO transactional, final TransactionStatus status) throws Exception {
                    final Integer pillDeleted = transactional.deletePillPairingByAccount(accessToken.accountId);
                    LOGGER.info("Factory reset delete {} Pills linked to account {}", pillDeleted, accessToken.accountId);

                    final Integer accountUnlinked = transactional.unlinkAllAccountsPairedToSense(senseId);
                    LOGGER.info("Factory reset delete {} accounts linked to Sense {}", accountUnlinked, accessToken.accountId);

                    for (final UserInfo userInfo : pairedUsers) {
                        try {
                            mergedUserInfoDynamoDB.unlinkAccountToDevice(userInfo.accountId, userInfo.deviceId);
                        } catch (AmazonServiceException awsEx) {
                            LOGGER.error("Failed to unlink account {} from Sense {} in merge user info. error {}",
                                    userInfo.accountId,
                                    userInfo.deviceId,
                                    awsEx.getErrorMessage());
                        }
                    }

                    return null;
                }
            });
        }catch (UnableToExecuteStatementException sqlExp){
            LOGGER.error("Failed to factory reset Sense {}, error {}", senseId, sqlExp.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // TODO: MOVE ALL ADMIN STUFF OUT OF HERE

    @POST
    @Path("/sense")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void registerSense(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final SenseRegistration senseRegistration) {
        try {
            final Long senseInternalId = deviceDAO.registerSense(accessToken.accountId, senseRegistration.senseId);
            LOGGER.info("Account {} registered sense {} with internal id = {}", accessToken.accountId, senseRegistration.senseId, senseInternalId);
            return;
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if(matcher.find()) {
                LOGGER.error("Failed to register sense for account id = {} and sense id = {} : {}", accessToken.accountId, senseRegistration.senseId, exception.getMessage());
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(new JsonError(409, "Sense already exists for this account.")).build());
            }
        }

    }

    @POST
    @Path("/pill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void registerPill(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final PillRegistration pillRegistration) {
        try {
            final Long trackerId = deviceDAO.registerPill(accessToken.accountId, pillRegistration.pillId);
            LOGGER.info("Account {} registered pill {} with internal id = {}", accessToken.accountId, pillRegistration.pillId, trackerId);

            final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accessToken.accountId);
            if(sensePairedWithAccount.size() == 0){
                LOGGER.error("No sense paired with account {}", accessToken.accountId);
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }

            final String senseId = sensePairedWithAccount.get(0).externalDeviceId;
            this.mergedUserInfoDynamoDB.setNextPillColor(senseId, accessToken.accountId, pillRegistration.pillId);

            return;
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());

            if(matcher.find()) {
                LOGGER.error("Failed to register pill for account id = {} and pill id = {} : {}", accessToken.accountId, pillRegistration.pillId, exception.getMessage());
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(new JsonError(409, "Pill already exists for this account.")).build());
            }
        } catch (AmazonServiceException awsEx){
            LOGGER.error("Set pill color failed for pill {}, error: {}", pillRegistration.pillId, awsEx.getMessage());
        }

        throw new WebApplicationException(Response.Status.BAD_REQUEST);
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
        final List<DeviceAccountPair> devices = deviceDAO.getSensesForAccountId(accountId.get());

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

    public static Device.Color getPillColor(final List<UserInfo> userInfoList, final Long accountId)  {
        // mutable state. argh
        Device.Color pillColor = Device.Color.BLUE;

        for(final UserInfo userInfo : userInfoList) {
            if(!accountId.equals(userInfo.accountId) || !userInfo.pillColor.isPresent()) {
                continue;
            }
            pillColor = PillColorUtil.displayDeviceColor(userInfo.pillColor.get().getPillColor());
        }

        return pillColor;
    }

    private List<Device> getDevicesByAccountId(final Long accountId) {
        // TODO: make asynchronous calls to grab Pills + Senses if the following is too slow
        final ImmutableList<DeviceAccountPair> senses = deviceDAO.getSensesForAccountId(accountId);
        final ImmutableList<DeviceAccountPair> pills = deviceDAO.getPillsForAccountId(accountId);
        final List<Device> devices = Lists.newArrayList();

        final List<UserInfo> userInfoList= Lists.newArrayList();
        if(!senses.isEmpty()) {
            userInfoList.addAll(mergedUserInfoDynamoDB.getInfo(senses.get(0).externalDeviceId));
        }else{
            return Collections.EMPTY_LIST;
        }


        final Device.Color pillColor = getPillColor(userInfoList, accountId);

        // TODO: device state will always be normal for now until more information is provided by the device

        for (final DeviceAccountPair sense : senses) {
            final Optional<DeviceStatus> senseStatusOptional = this.deviceDataDAO.senseStatus(sense.internalDeviceId);
            if(senseStatusOptional.isPresent()) {
                devices.add(new Device(Device.Type.SENSE, sense.externalDeviceId, Device.State.NORMAL, senseStatusOptional.get().firmwareVersion, senseStatusOptional.get().lastSeen, Device.Color.BLACK)); // TODO: grab Sense color from Serial Number
            } else {
                devices.add(new Device(Device.Type.SENSE, sense.externalDeviceId, Device.State.UNKNOWN, "-", null, Device.Color.BLACK));
            }
        }


        for (final DeviceAccountPair pill : pills) {
            final Optional<DeviceStatus> pillStatusOptional = this.trackerMotionDAO.pillStatus(pill.internalDeviceId);
            if(!pillStatusOptional.isPresent()) {
                LOGGER.debug("No pill status found for pill_id = {} ({}) for account: {}", pill.externalDeviceId, pill.internalDeviceId, pill.accountId);
                devices.add(new Device(Device.Type.PILL, pill.externalDeviceId, Device.State.UNKNOWN, null, null, pillColor));
            } else {
                final DeviceStatus deviceStatus = pillStatusOptional.get();
                devices.add(new Device(Device.Type.PILL, pill.externalDeviceId, Device.State.NORMAL, deviceStatus.firmwareVersion, deviceStatus.lastSeen, pillColor));
            }
        }

        return devices;
    }

    // Just for testing
    public List<Device> getDevices(final Long accountId) {
        return getDevicesByAccountId(accountId);
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
        Long devicesOnFirmware = 0L;

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



    @GET
    @Timed
    @Path("/inactive/sense")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceInactivePage getInactiveSenses(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                @QueryParam("after") final Long afterTimestamp,
                                                @QueryParam("before") final Long beforeTimestamp) {

        final RedisPaginator redisPaginator = new RedisPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.SENSE_ACTIVE_SET_KEY);
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

        final RedisPaginator redisPaginator = new RedisPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.PILL_ACTIVE_SET_KEY);
        final DeviceInactivePage inactivePillsPage = redisPaginator.generatePage();
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
}

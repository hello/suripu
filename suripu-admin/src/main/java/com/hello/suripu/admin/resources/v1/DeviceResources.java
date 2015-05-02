package com.hello.suripu.admin.resources.v1;

import com.amazonaws.AmazonServiceException;
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
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceInactivePage;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.PillRegistration;
import com.hello.suripu.core.models.ProvisionRequest;
import com.hello.suripu.core.models.SenseRegistration;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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
import java.util.regex.Matcher;

@Path("/v1/devices")
public class DeviceResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);

    private final DeviceDAO deviceDAO;
    private final DeviceDAOAdmin deviceDAOAdmin;
    private final DeviceDataDAO deviceDataDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AccountDAO accountDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final KeyStore senseKeyStore;
    private final KeyStore pillKeyStore;
    private final JedisPool jedisPool;
    private final PillHeartBeatDAO pillHeartBeatDAO;

    public DeviceResources(final DeviceDAO deviceDAO,
                           final DeviceDAOAdmin deviceDAOAdmin,
                           final DeviceDataDAO deviceDataDAO,
                           final TrackerMotionDAO trackerMotionDAO,
                           final AccountDAO accountDAO,
                           final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                           final KeyStore senseKeyStore,
                           final KeyStore pillKeyStore,
                           final JedisPool jedisPool,
                           final PillHeartBeatDAO pillHeartBeatDAO) {
        this.deviceDAO = deviceDAO;
        this.deviceDAOAdmin = deviceDAOAdmin;
        this.accountDAO = accountDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.senseKeyStore = senseKeyStore;
        this.pillKeyStore = pillKeyStore;
        this.deviceDataDAO = deviceDataDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.jedisPool = jedisPool;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
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
                                                @QueryParam("before") final Long beforeTimestamp,
                                                @QueryParam("limit") final Integer limit) {

        return new InactiveDevicesPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.SENSE_ACTIVE_SET_KEY, limit)
                .generatePage();
    }

    @GET
    @Timed
    @Path("/inactive/pill")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceInactivePage getInactivePills(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                               @QueryParam("after") final Long afterTimestamp,
                                               @QueryParam("before") final Long beforeTimestamp,
                                               @QueryParam("limit") final Integer limit) {

        InactiveDevicesPaginator inactiveDevicesPaginator;
        if (limit == null) {
            inactiveDevicesPaginator = new InactiveDevicesPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.PILL_ACTIVE_SET_KEY);
        }
        else {
            inactiveDevicesPaginator = new InactiveDevicesPaginator(jedisPool, afterTimestamp, beforeTimestamp, ActiveDevicesTrackerConfiguration.PILL_ACTIVE_SET_KEY, limit);
        }
        return inactiveDevicesPaginator.generatePage();
    }


    @POST
    @Path("/register/sense")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void registerSense(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                              @Valid final SenseRegistration senseRegistration) {

        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, senseRegistration.email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, String.format("Account %s not found", senseRegistration.email))).build());
        }
        final Long accountId = accountIdOptional.get();


        try {
            final Long senseInternalId = deviceDAO.registerSense(accountId, senseRegistration.senseId);
            LOGGER.info("Account {} registered sense {} with internal id = {}", accountId, senseRegistration.senseId, senseInternalId);
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if(matcher.find()) {
                LOGGER.error("Failed to register sense for account id = {} and sense id = {} : {}", accountId, senseRegistration.senseId, exception.getMessage());
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(new JsonError(409, "Sense already exists for this account.")).build());
            }
        }

        final List<DeviceAccountPair> deviceAccountMap = this.deviceDAO.getSensesForAccountId(accountId);

        for (final DeviceAccountPair deviceAccountPair:deviceAccountMap) {
            try {
                this.mergedUserInfoDynamoDB.setTimeZone(deviceAccountPair.externalDeviceId, accountId, DateTimeZone.forID(senseRegistration.timezone));
            } catch (AmazonServiceException awsException) {
                LOGGER.error("Aws failed when account {} tries to set timezone.", accountId);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new JsonError(500, "Failed to set timezone")).build());
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonError(400, "Unrecognized timezone")).build());
            }
        }
    }

    @POST
    @Path("/register/pill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void registerPill(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                             @Valid final PillRegistration pillRegistration) {

        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, pillRegistration.email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, String.format("Account %s not found", pillRegistration.email))).build());
        }
        final Long accountId = accountIdOptional.get();

        try {
            final Long trackerId = deviceDAO.registerPill(accountId, pillRegistration.pillId);
            LOGGER.info("Account {} registered pill {} with internal id = {}", accountId, pillRegistration.pillId, trackerId);

            final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accountId);
            if(sensePairedWithAccount.isEmpty()){
                LOGGER.error("No sense paired with account {}", accountId);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonError(400, String.format("Registered pill %s but no sense has been paired to account %s", pillRegistration.pillId, pillRegistration.email))).build());
            }

            final String senseId = sensePairedWithAccount.get(0).externalDeviceId;
            this.mergedUserInfoDynamoDB.setNextPillColor(senseId, accountId, pillRegistration.pillId);

            return;
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());

            if(matcher.find()) {
                LOGGER.error("Failed to register pill for account id = {} and pill id = {} : {}", accountId, pillRegistration.pillId, exception.getMessage());
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
                        .entity(new JsonError(409, "Pill already exists for this account.")).build());
            }
        } catch (AmazonServiceException awsEx){
            LOGGER.error("Set pill color failed for pill {}, error: {}", pillRegistration.pillId, awsEx.getMessage());
        }

        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }


    @DELETE
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sense/{email}/{sense_id}")
    public void unregisterSenseByUser(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                                      @PathParam("email") final String email,
                                      @PathParam("sense_id") final String senseId) {

        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, String.format("Account %s not found", email))).build());
        }
        final Long accountId = accountIdOptional.get();
        final List<UserInfo> pairedUsers = mergedUserInfoDynamoDB.getInfo(senseId);

        if (pairedUsers.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new JsonError(400, String.format("Sense %s has not been paired to any account", senseId))).build());
        }

        final List<Long> pairedAccountIdList = new ArrayList<>();
        for (final UserInfo pairUser: pairedUsers) {
            pairedAccountIdList.add(pairUser.accountId);
        }

        if (!pairedAccountIdList.contains(accountId)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new JsonError(400, String.format("Sense %s has not been paired to %s", senseId, email))).build());
        }


//            this.deviceDAO.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new Transaction<Void, DeviceDAO>() {
//                @Override
//                public Void inTransaction(final DeviceDAO transactional, final TransactionStatus status) throws Exception {
//                    final Integer pillDeleted = transactional.deletePillPairingByAccount(accountId);
//                    LOGGER.info("Factory reset delete {} Pills linked to account {}", pillDeleted, accountId);
//
//                    final Integer accountUnlinked = transactional.unlinkAllAccountsPairedToSense(senseId);
//                    LOGGER.info("Factory reset delete {} accounts linked to Sense {}", accountUnlinked, accountId);
//
//                    try {
//                        mergedUserInfoDynamoDB.unlinkAccountToDevice(accountId, senseId);
//                    } catch (AmazonServiceException awsEx) {
//                        LOGGER.error("Failed to unlink account {} from Sense {} in merge user info. error {}",
//                                accountId,
//                                senseId,
//                                awsEx.getErrorMessage());
//                    }
//
//                    return null;
//                }
//            });

        try {
            deviceDAO.unlinkAllAccountsPairedToSense(senseId);
            mergedUserInfoDynamoDB.unlinkAccountToDevice(accountId, senseId);
        }
        catch (AmazonServiceException awsEx) {
            LOGGER.error("Failed to unlink account {} from Sense {} in merge user info. error {}",
                    accountId,
                    senseId,
                    awsEx.getErrorMessage());
        }catch (UnableToExecuteStatementException sqlExp){
            LOGGER.error("Failed to factory reset Sense {}, error {}", senseId, sqlExp.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @DELETE
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/pill/{email}/{pill_id}")
    public void unregisterPill(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                               @PathParam("email") final String email,
                               @PathParam("pill_id") String externalPillId) {

        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, String.format("Account %s not found", email))).build());
        }
        final Long accountId = accountIdOptional.get();

        final Integer numRows = deviceDAO.deletePillPairing(externalPillId, accountId);
        if(numRows == 0) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, String.format("Did not find active pill %s to unregister for %s", externalPillId, email))).build());
        }

        final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accountId);
        if(sensePairedWithAccount.size() == 0){
            LOGGER.error("No sense paired with account {}", accountId);
            return;
        }

        final String senseId = sensePairedWithAccount.get(0).externalDeviceId;

        try {
            this.mergedUserInfoDynamoDB.deletePillColor(senseId, accountId, externalPillId);
        }catch (Exception ex){
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404,
                            String.format("Failed to delete pill %s color from user info table for sense %s and account %s because %s",
                                    externalPillId, senseId, accountId, ex.getMessage()))).build());
        }
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


    @POST
    @Path("/provision/sense")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void senseProvision(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final ProvisionRequest provisionRequest) {
        senseKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
    }


    @POST
    @Path("/provision/pill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void pillProvision(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final ProvisionRequest provisionRequest) {
        pillKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
    }


    @POST
    @Path("/provision/batch_pills")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void batchPillsProvision(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken, @Valid final List<ProvisionRequest> provisionRequests) {
        for (final ProvisionRequest provisionRequest : provisionRequests) {
            pillKeyStore.put(provisionRequest.deviceId, provisionRequest.publicKey, provisionRequest.metadata);
        }
    }

    @GET
    @Path("/timezone")
    @Produces(MediaType.APPLICATION_JSON)
    public TimeZoneHistory getTimezone(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                       @QueryParam("sense_id") final String senseId,
                                       @QueryParam("email") final String email,
                                       @QueryParam("event_ts") final Long eventTs){

        if (senseId == null && email == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Require sense_id OR email!").build());
        }
        final DateTime eventDateTime = eventTs == null ? DateTime.now(DateTimeZone.UTC) : new DateTime(eventTs);

        final Optional<TimeZoneHistory> timeZoneHistoryOptional = (senseId != null) ?
            getTimeZoneBySenseId(senseId, eventDateTime) : getTimeZoneByEmail(email, eventDateTime);

        if (!timeZoneHistoryOptional.isPresent()){
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(404, "Failed to retrieve timezone")).build());
        }
        return timeZoneHistoryOptional.get();
    }


    // Helpers
    private List<DeviceAdmin> getSensesByAccountId(final Long accountId) {
        final ImmutableList<DeviceAccountPair> senseAccountPairs = deviceDAO.getSensesForAccountId(accountId);
        final List<DeviceAdmin> senses = new ArrayList<>();

        for (final DeviceAccountPair senseAccountPair: senseAccountPairs) {
            Optional<DeviceStatus> senseStatusOptional = this.deviceDataDAO.senseStatusLastHour(senseAccountPair.internalDeviceId);
            if (!senseStatusOptional.isPresent()) {
                senseStatusOptional = this.deviceDataDAO.senseStatus(senseAccountPair.internalDeviceId);
            }
            senses.add(new DeviceAdmin(senseAccountPair, senseStatusOptional.orNull()));
        }
        return senses;
    }

    private List<DeviceAdmin> getPillsByAccountId(final Long accountId) {
        final ImmutableList<DeviceAccountPair> pillAccountPairs = deviceDAO.getPillsForAccountId(accountId);
        final List<DeviceAdmin> pills = new ArrayList<>();

        for (final DeviceAccountPair pillAccountPair: pillAccountPairs) {
            Optional<DeviceStatus> pillStatusOptional = this.pillHeartBeatDAO.getPillStatus(pillAccountPair.internalDeviceId);
            if (!pillStatusOptional.isPresent()){
                pillStatusOptional = this.trackerMotionDAO.pillStatus(pillAccountPair.internalDeviceId);
            }
            pills.add(new DeviceAdmin(pillAccountPair, pillStatusOptional.orNull()));
        }
        return pills;
    }

    private Optional<TimeZoneHistory> getTimeZoneBySenseId(final String senseId, final DateTime eventDateTime) {
        final List <UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(senseId);
        if (userInfoList.isEmpty()) {
            return Optional.absent();
        }
        final Optional<DateTimeZone> dateTimeZoneOptional = mergedUserInfoDynamoDB.getTimezone(senseId, userInfoList.get(0).accountId);

        if (!dateTimeZoneOptional.isPresent()) {
            return Optional.absent();
        }
        return Optional.of(new TimeZoneHistory(dateTimeZoneOptional.get().getOffset(eventDateTime), dateTimeZoneOptional.get().getID()));
    }

    private Optional<TimeZoneHistory> getTimeZoneByEmail(final String email, final DateTime eventDateTime) {
        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            return Optional.absent();
        }

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountIdOptional.get());
        if (!deviceAccountPairOptional.isPresent()) {
            return Optional.absent();
        }
        final Optional<DateTimeZone> dateTimeZoneOptional = mergedUserInfoDynamoDB.getTimezone(deviceAccountPairOptional.get().externalDeviceId, accountIdOptional.get());

        if (!dateTimeZoneOptional.isPresent()) {
            return Optional.absent();
        }
        return Optional.of(new TimeZoneHistory(dateTimeZoneOptional.get().getOffset(eventDateTime), dateTimeZoneOptional.get().getID()));
    }
}

package com.hello.suripu.app.v2;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.app.resources.v1.DeviceResources;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.PairingInfo;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.models.device.v2.Devices;
import com.hello.suripu.core.models.device.v2.DevicesRetriever;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.JsonError;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v2/devices")
public class DeviceResource extends BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);

    private static final Integer PILL_BATTERY_ALERT_THRESHOLD = 10;

    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final PillHeartBeatDAO pillHeartBeatDAO;
    private final WifiInfoDAO wifiInfoDAO;
    private final SenseColorDAO senseColorDAO;

    @Inject
    RolloutClient feature;

    public DeviceResource(final DeviceDAO deviceDAO,
                          final DeviceDataDAO deviceDataDAO,
                          final TrackerMotionDAO trackerMotionDAO,
                          final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                          final PillHeartBeatDAO pillHeartBeatDAO,
                          final SensorsViewsDynamoDB sensorsViewsDynamoDB,
                          final WifiInfoDAO wifiInfoDAO,
                          final SenseColorDAO senseColorDAO) {

        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.deviceDataDAO = deviceDataDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.wifiInfoDAO = wifiInfoDAO;
        this.senseColorDAO = senseColorDAO;
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
    public Devices getDevices(@Scope(OAuthScope.DEVICE_INFORMATION_READ) final AccessToken accessToken) {

        final DevicesRetriever devicesRetriever = new DevicesRetriever(deviceDAO, deviceDataDAO, mergedUserInfoDynamoDB, sensorsViewsDynamoDB, pillHeartBeatDAO, trackerMotionDAO, wifiInfoDAO, senseColorDAO, this.isSenseLastSeenDynamoDBReadEnabled(accessToken.accountId), this.isSensorsDBUnavailable(accessToken.accountId));
        return devicesRetriever.getAllDevices(accessToken.accountId);

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

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/wifi_info")
    public Response updateWifiInfo(@Scope(OAuthScope.DEVICE_INFORMATION_WRITE) final AccessToken accessToken,
                                   final WifiInfo wifiInfo){
        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accessToken.accountId);

        if (deviceAccountPairOptional.isPresent()) {
            final String mostRecentlyPairedSenseId = deviceAccountPairOptional.get().externalDeviceId;
            if (mostRecentlyPairedSenseId.equals(wifiInfo.senseId)) {
                if (wifiInfoDAO.put(wifiInfo)) {
                    return Response.noContent().build();
                }
            }
            else {
                LOGGER.debug("Account {} attempted to update wifi info for not sense {} but the most recently paired sense is {}",
                        accessToken.accountId, wifiInfo.senseId, mostRecentlyPairedSenseId);
            }
        }
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new JsonError(500, "Internal server error")).build());
    }
}

package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.models.DeviceAdmin;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
    private final DeviceDataDAO deviceDataDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AccountDAO accountDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final KeyStore senseKeyStore;
    private final KeyStore pillKeyStore;

    public DeviceResources(final DeviceDAO deviceDAO,
                           final DeviceDataDAO deviceDataDAO,
                           final TrackerMotionDAO trackerMotionDAO,
                           final AccountDAO accountDAO,
                           final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                           final KeyStore senseKeyStore,
                           final KeyStore pillKeyStore) {
        this.deviceDAO = deviceDAO;
        this.accountDAO = accountDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.senseKeyStore = senseKeyStore;
        this.pillKeyStore = pillKeyStore;
        this.deviceDataDAO = deviceDataDAO;
        this.trackerMotionDAO = trackerMotionDAO;
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
            pills.addAll(deviceDAO.getPillsByPillIdHint(pillIdPartial));
        }

        final List<DeviceStatus> pillStatuses = new ArrayList<>();
        for (final DeviceAccountPair pill : pills) {
            pillStatuses.addAll(deviceDAO.pillStatusBeforeTs(pill.internalDeviceId, new DateTime(endTs, DateTimeZone.UTC)));
        }

        return pillStatuses;
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

package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.admin.Util;
import com.hello.suripu.admin.models.Pill;
import com.hello.suripu.admin.models.Sense;
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
    public List<Sense> getSensesByEmail(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
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
    public List<Pill> getPillsByEmail(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                       @QueryParam("email") final String email) {
        LOGGER.debug("Querying all pills for email = {}", email);
        final Optional<Long> accountIdOptional = Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("Account not found!").build());
        }
        return getPillsByAccountId(accountIdOptional.get());
    }

    // Helpers
    private List<Sense> getSensesByAccountId(final Long accountId) {
        final ImmutableList<DeviceAccountPair> senseAccountPairs = deviceDAO.getSensesForAccountId(accountId);
        final List<Sense> senses = new ArrayList<>();

        for (final DeviceAccountPair senseAccountPair: senseAccountPairs) {
            final Optional<DeviceStatus> senseStatusOptional = this.deviceDataDAO.senseStatus(senseAccountPair.internalDeviceId);
            if(!senseStatusOptional.isPresent()) {
                final DeviceStatus senseStatus = senseStatusOptional.get();
                senses.add(new Sense(senseAccountPair.externalDeviceId, Sense.State.NORMAL, senseStatus.firmwareVersion, senseStatus.lastSeen, senseAccountPair.created));
            } else {
                if (senseAccountPair.created.plusHours(SENSE_STATUS_WAITING_HOURS).isAfterNow()) {
                    senses.add(new Sense(senseAccountPair.externalDeviceId, Sense.State.WAITING, "-", senseAccountPair.created, senseAccountPair.created));
                }
                else {
                    senses.add(new Sense(senseAccountPair.externalDeviceId, Sense.State.UNKNOWN, "-", senseAccountPair.created, senseAccountPair.created));
                }
            }
        }
        return senses;
    }

    private List<Pill> getPillsByAccountId(final Long accountId) {
        final ImmutableList<DeviceAccountPair> pillAccountPairs = deviceDAO.getPillsForAccountId(accountId);
        final List<Pill> pills = new ArrayList<>();

        for (final DeviceAccountPair pillAccountPair: pillAccountPairs) {
            final Optional<DeviceStatus> pillStatusOptional = this.trackerMotionDAO.pillStatus(pillAccountPair.internalDeviceId);
            if(pillStatusOptional.isPresent()) {
                final DeviceStatus pillStatus = pillStatusOptional.get();
                pills.add(new Pill(pillAccountPair.externalDeviceId, Pill.State.NORMAL, pillStatus.batteryLevel, pillStatus.uptime, pillStatus.lastSeen, pillAccountPair.created));
            } else {
                if (pillAccountPair.created.plusHours(PILL_STATUS_WAITING_HOURS).isAfterNow()) {
                    pills.add(new Pill(pillAccountPair.externalDeviceId, Pill.State.WAITING, -1, -1, pillAccountPair.created, pillAccountPair.created));
                }
                else {
                    pills.add(new Pill(pillAccountPair.externalDeviceId, Pill.State.UNKNOWN, -1, -1, pillAccountPair.created, pillAccountPair.created));
                }
            }
        }
        return pills;
    }
}

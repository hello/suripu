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
import com.hello.suripu.core.models.DeviceInfo;
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
        final ImmutableList<DeviceInfo> senseInfoList = deviceDAO.getSensesForAccountIdAdmin(accountId);
        final List<Sense> senses = new ArrayList<>();

        for (final DeviceInfo senseInfo: senseInfoList) {
            final Optional<DeviceStatus> senseStatusOptional = this.deviceDataDAO.senseStatus(senseInfo.id);
            if(!senseStatusOptional.isPresent()) {
                final DeviceStatus senseStatus = senseStatusOptional.get();
                senses.add(new Sense(senseInfo.deviceId, Sense.State.NORMAL, senseStatus.firmwareVersion, senseStatus.lastSeen, senseInfo.created));
            } else {
                if (senseInfo.created.plusHours(SENSE_STATUS_WAITING_HOURS).isAfterNow()) {
                    senses.add(new Sense(senseInfo.deviceId, Sense.State.WAITING, "-", senseInfo.created, senseInfo.created));
                }
                else {
                    senses.add(new Sense(senseInfo.deviceId, Sense.State.UNKNOWN, "-", senseInfo.created, senseInfo.created));
                }
            }
        }
        return senses;
    }

    private List<Pill> getPillsByAccountId(final Long accountId) {
        final ImmutableList<DeviceInfo> pillInfoList = deviceDAO.getPillsForAccountIdAdmin(accountId);
        final List<Pill> pills = new ArrayList<>();

        for (final DeviceInfo pillInfo: pillInfoList) {
            final Optional<DeviceStatus> pillStatusOptional = this.trackerMotionDAO.pillStatus(pillInfo.id);
            if(pillStatusOptional.isPresent()) {
                final DeviceStatus pillStatus = pillStatusOptional.get();
                pills.add(new Pill(pillInfo.deviceId, Pill.State.NORMAL, pillStatus.batteryLevel, pillStatus.uptime, pillStatus.lastSeen, pillInfo.created));
            } else {
                if (pillInfo.created.plusHours(PILL_STATUS_WAITING_HOURS).isAfterNow()) {
                    pills.add(new Pill(pillInfo.deviceId, Pill.State.WAITING, -1, -1, pillInfo.created, pillInfo.created));
                }
                else {
                    pills.add(new Pill(pillInfo.deviceId, Pill.State.UNKNOWN, -1, -1, pillInfo.created, pillInfo.created));
                }
            }
        }
        return pills;
    }
}

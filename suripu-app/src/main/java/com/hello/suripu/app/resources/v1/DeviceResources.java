package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
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

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
    private final AccountDAO accountDAO;

    public DeviceResources(final DeviceDAO deviceDAO,
                           final AccountDAO accountDAO) {
        this.deviceDAO = deviceDAO;
        this.accountDAO = accountDAO;
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

        // TODO: make asynchronous calls to grab Pills + Senses if the following is too slow
        final ImmutableList<DeviceAccountPair> senses = deviceDAO.getSensesForAccountId(accessToken.accountId);
        final ImmutableList<DeviceAccountPair> pills = deviceDAO.getPillsForAccountId(accessToken.accountId);
        final List<Device> devices = new ArrayList<Device>();

        // TODO: device state will always be normal for now until more information is provided by the device
        for (final DeviceAccountPair sense : senses) {
            devices.add(new Device(Device.Type.SENSE, sense.externalDeviceId, Device.State.NORMAL, "alpha-1", DateTime.now()));
        }

        for (final DeviceAccountPair pill : pills) {
            final Optional<DeviceStatus> pillStatusOptional = deviceDAO.pillStatus(pill.internalDeviceId);
            if(!pillStatusOptional.isPresent()) {
                LOGGER.warn("No pill status found for pill_id = {} ({}) for account: {}", pill.externalDeviceId, pill.internalDeviceId, pill.accountId);
                devices.add(new Device(Device.Type.PILL, pill.externalDeviceId, Device.State.UNKNOWN, null, null));
            } else {
                final DeviceStatus deviceStatus = pillStatusOptional.get();
                devices.add(new Device(Device.Type.PILL, pill.externalDeviceId, Device.State.NORMAL, deviceStatus.firmwareVersion, deviceStatus.lastSeen));
            }
        }

        return devices;
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
        if(numRows == 0) {
            LOGGER.warn("Did not find active sense to unregister");
        }
    }
}

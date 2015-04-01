package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.admin.models.UserInteraction;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;


@Path("/v1/data")
public class DataResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataResources.class);
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final AccountDAO accountDAO;

    public DataResources(final DeviceDataDAO deviceDataDAO, final DeviceDAO deviceDAO, final AccountDAO accountDAO) {
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.accountDAO = accountDAO;

    }

    @GET
    @Path("/user_interaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserInteraction> getUserInteractions(@Scope({OAuthScope.SENSORS_BASIC, OAuthScope.RESEARCH}) final AccessToken accessToken,
                                                     @QueryParam("email") String email,
                                                     @QueryParam("account_id") Long accountId,
                                                     @QueryParam("start_ts") Long startTimestamp,
                                                     @QueryParam("end_ts") Long endTimestamp) {

        if ( (email == null && accountId == null) || (startTimestamp == null || endTimestamp == null) ) {
            throw new WebApplicationException(Response.status(400).entity(new JsonError(400,
                "Missing query parameters, use email or account_id, and start_ts and end_ts")).build());
        }

        Optional<Account> optionalAccount;
        if (email != null) {
            optionalAccount = accountDAO.getByEmail(email);
        } else {
            optionalAccount = accountDAO.getById(accountId);
        }

        if (!optionalAccount.isPresent() || !optionalAccount.get().id.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final Account account = optionalAccount.get();
        LOGGER.debug("Getting user interactions for account {} between {} and {}", account.id.get(), startTimestamp, endTimestamp);

        return getUserInteractionsData(account.id.get(), startTimestamp, endTimestamp);
    }


    private List<UserInteraction> getUserInteractionsData(final Long accountId, final Long startTimestamp, final Long endTimestamp) {

        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if (!deviceAccountPairOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity("This account does not have a sense recently").build());
        }

        final Long timeRangeLimitMillis = 3 * 86400 * 1000L;
        if ( (endTimestamp - startTimestamp) >  timeRangeLimitMillis) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Maximum time range (3 days) exceeded").build());
        }

        final int slotDurationInMinutes = 5;
        final Integer missingDataDefaultValue = 0;
        final AllSensorSampleList sensorSamples = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(
                startTimestamp,
                endTimestamp,
                accountId,
                deviceAccountPairOptional.get().internalDeviceId,
                slotDurationInMinutes,
                missingDataDefaultValue
        );

        final List<UserInteraction> userInteractions = new ArrayList<>();

        LOGGER.info("{}", sensorSamples.getAvailableSensors());

        final List<Sample> waveCountData = sensorSamples.get(Sensor.WAVE_COUNT);
        final List<Sample> holdCountData = sensorSamples.get(Sensor.HOLD_COUNT);
        LOGGER.info("wave size {}", waveCountData.size());
        LOGGER.info("hold size {}", holdCountData.size());
        for (int i=0; i<waveCountData.size(); i++) {
            userInteractions.add(new UserInteraction(
                waveCountData.get(i).value,
                holdCountData.get(i).value,
                waveCountData.get(i).dateTime,
                waveCountData.get(i).offsetMillis
            ));
        }

        return userInteractions;
    }
}

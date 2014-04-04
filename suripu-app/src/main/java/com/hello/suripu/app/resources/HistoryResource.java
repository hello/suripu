package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.Record;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.TimeSerieDAO;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/history")
public class HistoryResource {


    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryResource.class);
    private final TimeSerieDAO timeSerieDAO;
    private final DeviceDAO deviceDAO;

    public HistoryResource(final TimeSerieDAO timeSerieDAO, final DeviceDAO deviceDAO) {
        this.timeSerieDAO = timeSerieDAO;
        this.deviceDAO = deviceDAO;
    }

    @GET
    @Timed
    @Path("/{days}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Record> getRecords(
            @Scope({OAuthScope.SENSORS_BASIC}) final AccessToken accessToken,
            @PathParam("days") final Integer numDays) {

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime then = now.minusDays(numDays);

        final Optional<Long> deviceId = deviceDAO.getByAccountId(accessToken.accountId);
        LOGGER.debug("Account id = {}", accessToken.accountId);
        if(!deviceId.isPresent()) {
            throw new WebApplicationException(404);
        }
        LOGGER.debug("device = {}", deviceId.get());
        final ImmutableList<Record> records = timeSerieDAO.getHistoricalData(deviceId.get(), then, now);
        return records;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public List<Record> getRecordsBetween(@QueryParam("from") Long from, @QueryParam("to") Long to) {

        return new ArrayList<Record>();
    }
}

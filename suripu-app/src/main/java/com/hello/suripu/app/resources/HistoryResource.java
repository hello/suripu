package com.hello.suripu.app.resources;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.Record;
import com.hello.suripu.core.db.TimeSerieDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/history")
public class HistoryResource {

    private final TimeSerieDAO timeSerieDAO;

    public HistoryResource(final TimeSerieDAO timeSerieDAO) {
        this.timeSerieDAO = timeSerieDAO;
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

        final ImmutableList<Record> records = timeSerieDAO.getHistoricalData(accessToken.accountId, then, now);
        return records;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public List<Record> getRecordsBetween(@QueryParam("from") Long from, @QueryParam("to") Long to) {

        return new ArrayList<Record>();
    }
}

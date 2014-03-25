package com.hello.suripu.app.resources;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.Record;
import com.hello.suripu.core.db.TimeSerieDAO;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
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
            @Scope({OAuthScope.USER_EXTENDED, OAuthScope.USER_BASIC}) final ClientDetails clientDetails,
            @PathParam("days") final Integer numDays) {

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime then = now.minusDays(numDays);

        final ImmutableList<Record> records = timeSerieDAO.getHistoricalData(clientDetails.accountId, then, now);
        return records;
    }
}

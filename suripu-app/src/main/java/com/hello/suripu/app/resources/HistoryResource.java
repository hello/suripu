package com.hello.suripu.app.resources;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.Record;
import com.hello.suripu.core.db.TimeSerieDAO;
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
    @Path("/{days}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Record> getRecords(@PathParam("days") Integer numDays) {

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime then = now.minusDays(numDays);

        ImmutableList<Record> records = timeSerieDAO.getHistoricalData(123L, then, now);
        return records;
    }
}

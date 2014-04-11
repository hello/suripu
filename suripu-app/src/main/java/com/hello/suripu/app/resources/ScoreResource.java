package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.Record;
import com.hello.suripu.core.Score;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.TimeSerieDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/score")
public class ScoreResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreResource.class);

    private final TimeSerieDAO timeSerieDAO;
    private final DeviceDAO deviceDAO;

    public ScoreResource(final TimeSerieDAO timeSerieDAO, final DeviceDAO deviceDAO) {
        this.timeSerieDAO = timeSerieDAO;
        this.deviceDAO = deviceDAO;
    }


    private int computeScore(List<Record> records) {
        int score = 100;

        for(Record record : records) {
            if(record.ambientLight > 100000) {
                score -= 1;
            }
        }
        return Math.max(0, score);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMostRecent(@Scope({OAuthScope.SCORE_READ}) final AccessToken token) {

        Optional<Long> deviceIdOptional = deviceDAO.getByAccountId(token.accountId);
        if(!deviceIdOptional.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).entity("not found").build();
        }
        DateTime now = DateTime.now(DateTimeZone.UTC);
        DateTime then = now.minusDays(7);

        List<Record> records = timeSerieDAO.getHistoricalData(deviceIdOptional.get(), now, then);

        int computedScore = computeScore(records);
        LOGGER.debug("Computed score = {}", computedScore);

        final Score score = new Score(98, 96, 60,50,computedScore, DateTime.now(DateTimeZone.UTC));
        LOGGER.debug(score.toString());
        return Response.ok().entity(score).build();
    }

    @GET
    @Path("/{num_days}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getForLastNumDays(
            @Scope({OAuthScope.SCORE_READ}) final AccessToken token,
            @PathParam("num_days") final Integer numDays) {

        final List<Score> scores = new ArrayList<Score>(10);

        for(int i =0; i < 10; i ++) {
            final Score score = new Score(98 + i, 96 + i,60 +i ,80 +i ,12 + i, DateTime.now(DateTimeZone.UTC));
            scores.add(score);
        }

        return Response.ok().entity(scores).build();
    }
}

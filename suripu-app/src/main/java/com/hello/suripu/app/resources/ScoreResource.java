package com.hello.suripu.app.resources;

import com.hello.suripu.core.Score;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMostRecent(@Scope({OAuthScope.SCORE_READ}) final AccessToken token) {
        final Score score = new Score(98, 96,60,80,12, DateTime.now(DateTimeZone.UTC));
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

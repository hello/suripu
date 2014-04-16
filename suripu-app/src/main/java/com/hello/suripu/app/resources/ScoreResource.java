package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.Score;
import com.hello.suripu.core.SoundRecord;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.ScoreDAO;
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
    private final ScoreDAO scoreDAO;

    public ScoreResource(final TimeSerieDAO timeSerieDAO, final DeviceDAO deviceDAO, final ScoreDAO scoreDAO) {
        this.timeSerieDAO = timeSerieDAO;
        this.deviceDAO = deviceDAO;
        this.scoreDAO = scoreDAO;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMostRecent(@Scope({OAuthScope.SCORE_READ}) final AccessToken token) {

        Optional<Long> deviceIdOptional = deviceDAO.getByAccountId(token.accountId);
        if(!deviceIdOptional.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).entity("not found").build();
        }

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime then = now.minusDays(2);

        final List<SoundRecord> soundRecords = timeSerieDAO.getAvgSoundData(deviceIdOptional.get(), then, now);
        LOGGER.debug("Got {} records", soundRecords.size());

        int soundScore = computeSoundScore(soundRecords);
        final Score score = new Score.Builder()
                .withAccountId(token.accountId)
                .withSound(soundScore)
                .build();

        LOGGER.debug(score.toString());

        return Response.ok().entity(score).build();
    }

    private int computeSoundScore(List<SoundRecord> soundRecords) {
        if(soundRecords.isEmpty()) {
            return 0;
        }

        int score = 100;
        int sum = 0;

        for(SoundRecord record : soundRecords) {
            sum += Math.round(record.averageMaxAmplitude);
        }

        float mean = sum / soundRecords.size();

        final List<Float> devsFromMean = new ArrayList<Float>(soundRecords.size());
        final List<Float> squareDevsFromMean = new ArrayList<Float>(soundRecords.size());

        for(SoundRecord record : soundRecords) {
            devsFromMean.add(record.averageMaxAmplitude - mean);
        }

        for(Float sample: devsFromMean) {
            squareDevsFromMean.add((float) Math.pow(sample, 2));
        }

        float sumDeviations = 0;
        for(Float sample : squareDevsFromMean) {
            sumDeviations += sample;
        }

        float stddev = (float) Math.sqrt(sumDeviations / (soundRecords.size() - 1));

        for(SoundRecord soundRecord : soundRecords) {
            if(Math.abs(soundRecord.averageMaxAmplitude - mean) > stddev * 0.75) {
                score -= 1;
            }
        }

        return score;
    }
    @GET
    @Path("/recent")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMostRecentScores(@Scope({OAuthScope.SCORE_READ}) final AccessToken token) {

        final Optional<Long> deviceIdOptional = deviceDAO.getByAccountId(token.accountId);
        if(!deviceIdOptional.isPresent()) {
            LOGGER.debug("Didn't find deviceId");
            return Response.status(Response.Status.NOT_FOUND).entity("not found").build();
        }

        final List<Score> scores = scoreDAO.getRecentScores(token.accountId);
        LOGGER.debug("Found {} scores", scores.size());
        return Response.ok().entity(scores).build();
    }

    @GET
    @Path("/{num_days}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getForLastNumDays(
            @Scope({OAuthScope.SCORE_READ}) final AccessToken token,
            @PathParam("num_days") final Integer numDays) {

        final List<Score> scores = new ArrayList<Score>(10);

        for(int i =0; i < 10; i ++) {
            final Score score = new Score(token.accountId, 98 + i, 96 + i,60 +i ,80 +i ,12 + i, DateTime.now(DateTimeZone.UTC));
            scores.add(score);
        }

        return Response.ok().entity(scores).build();
    }
}

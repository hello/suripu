package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.SleepScoreProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by kingshy on 10/1/14.
 */

@Path("/v1/scores/")
public class ScoresResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScoresResource.class);

    private final TrackerMotionDAO trackerMotionDAO;
    private final SleepScoreDAO sleepScoreDAO;
    private final SleepLabelDAO sleepLabelDAO;
    private final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB;
    private final int dateBucketPeriod;
    private final String sleepScoreVersion;

    public ScoresResource(final TrackerMotionDAO trackerMotionDAO,
                          final SleepLabelDAO sleepLabelDAO,
                          final SleepScoreDAO sleepScoreDAO,
                          final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB,
                          final int dateBucketPeriod,
                          final String sleepScoreVersion) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.sleepLabelDAO = sleepLabelDAO;
        this.sleepScoreDAO = sleepScoreDAO;
        this.aggregateSleepScoreDAODynamoDB = aggregateSleepScoreDAODynamoDB;
        this.dateBucketPeriod = dateBucketPeriod;
        this.sleepScoreVersion = sleepScoreVersion;
    }

    @Timed
    @Path("/sleep/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<AggregateScore> getSleepScores(
            @Scope(OAuthScope.SLEEP_TIMELINE) final AccessToken accessToken,
            @PathParam("date") String date,
            @QueryParam("days") Integer days
    ) {

        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();

        LOGGER.debug("Target Date: {}", targetDate);

        final List<AggregateScore> scores = SleepScoreProcessor.getSleepScores(accessToken.accountId, targetDate, days,
                this.trackerMotionDAO,
                this.sleepLabelDAO,
                this.sleepScoreDAO,
                this.aggregateSleepScoreDAODynamoDB,
                this.dateBucketPeriod,
                this.sleepScoreVersion);

        return scores;
    }
}

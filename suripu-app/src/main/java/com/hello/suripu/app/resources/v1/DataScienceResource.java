package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by pangwu on 12/1/14.
 */
@Path("/v1/datascience")
public class DataScienceResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataScienceResource.class);
    private final TrackerMotionDAO trackerMotionDAO;


    public DataScienceResource(final TrackerMotionDAO trackerMotionDAO){
        this.trackerMotionDAO = trackerMotionDAO;
    }

    @GET
    @Path("/pill/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> registerPill(@Scope(OAuthScope.SENSORS_BASIC) final AccessToken accessToken,
                             @PathParam("date") String date) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(14);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accessToken.accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        return trackerMotions;
    }
}

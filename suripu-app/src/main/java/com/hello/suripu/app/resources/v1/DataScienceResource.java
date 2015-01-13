package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.TimelineUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created by pangwu on 12/1/14.
 */
@Path("/v1/datascience")
public class DataScienceResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataScienceResource.class);
    private final AccountDAO accountDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDAO deviceDAO;
    private final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final TrendsDAO trendsDAO;
    private final QuestionResponseDAO questionResponseDAO;
    private final LightData lightData;

    public DataScienceResource(final AccountDAO accountDAO,
                               final TrackerMotionDAO trackerMotionDAO,
                               final DeviceDataDAO deviceDataDAO,
                               final DeviceDAO deviceDAO,
                               final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB,
                               final InsightsDAODynamoDB insightsDAODynamoDB,
                               final TrendsDAO trendsDAO,
                               final QuestionResponseDAO questionResponseDAO,
                               final LightData lightData){
        this.accountDAO = accountDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDAO = deviceDAO;
        this.aggregateSleepScoreDAODynamoDB = aggregateSleepScoreDAODynamoDB;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.trendsDAO = trendsDAO;
        this.questionResponseDAO = questionResponseDAO;
        this.lightData = lightData;
    }

    @GET
    @Path("/pill/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackerMotion> getMotion(@Scope(OAuthScope.SENSORS_BASIC) final AccessToken accessToken,
                             @PathParam("query_date_local_utc") String date) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accessToken.accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        return trackerMotions;
    }

    @GET
    @Path("/light/{query_date_local_utc}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getLightOut(@Scope(OAuthScope.SENSORS_BASIC) final AccessToken accessToken,
                                            @PathParam("query_date_local_utc") String date) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final Optional<Long> internalSenseIdOptional = this.deviceDAO.getMostRecentSenseByAccountId(accessToken.accountId);

        if(!internalSenseIdOptional.isPresent()){
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final List<Sample> senseData = deviceDataDAO.generateTimeSeriesByLocalTime(targetDate.getMillis(),
                endDate.getMillis(), accessToken.accountId, internalSenseIdOptional.get(), 1, "light");

        final List<Event> lightEvents = TimelineUtils.getLightEvents(senseData);

        return lightEvents;
    }

    // TODO: rm later. temporary endpoint to create insights
    @PUT
    @Path("insights/{category}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createLightInsight(@Scope(OAuthScope.SENSORS_BASIC) final AccessToken accessToken,
                                   @PathParam("category") int value) {

        final InsightCard.Category category = InsightCard.Category.fromInteger(value);

        final AccountInfoProcessor.Builder builder = new AccountInfoProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withMapping(questionResponseDAO);
        final AccountInfoProcessor accountInfoProcessor = builder.build();

        final InsightProcessor processor = new InsightProcessor(deviceDataDAO, deviceDAO, trendsDAO, trackerMotionDAO, aggregateSleepScoreDAODynamoDB, insightsDAODynamoDB, accountInfoProcessor, lightData);
        final Optional<Account> accountOptional = accountDAO.getById(accessToken.accountId);
        if (accountOptional.isPresent()) {
            final Long accountId = accountOptional.get().id.get();

            final Optional<Long> deviceIdOptional = deviceDAO.getMostRecentSenseByAccountId(accountId);
            if (!deviceIdOptional.isPresent()) {
                return;
            }

            processor.generateInsights(accountId, accountOptional.get().created);

//            if (category == InsightCard.Category.LIGHT || category == InsightCard.Category.TEMPERATURE) {
//                processor.generateInsightsByCategory(accountId, deviceIdOptional.get(), category);
//            } else {
//                processor.generateInsights(accountId, accountOptional.get().created);
//            }
        }
    }
}

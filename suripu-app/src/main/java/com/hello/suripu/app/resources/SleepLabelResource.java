package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.SleepLabel;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by pangwu on 4/16/14.
 */
@Path("/sleep")
public class SleepLabelResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepLabelResource.class);

    private final SleepLabelDAO sleepLabelDAO;
    public SleepLabelResource(final SleepLabelDAO sleepLabelDAO){
        this.sleepLabelDAO = sleepLabelDAO;
    }

    @Path("/get")
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLabel(@QueryParam("target_date_millis") final Long targetDayTimestamp,
                             @QueryParam("timezone_offset_millis") final Integer timeZoneOffset,
                             @Scope({OAuthScope.SLEEP_LABEL_BASIC}) final AccessToken accessToken){
        if(targetDayTimestamp == null || timeZoneOffset == null){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final DateTimeZone userLocalTimeZone = DateTimeZone.forOffsetMillis(timeZoneOffset);
        final DateTime userLocalDateTime = new DateTime(targetDayTimestamp, userLocalTimeZone).withTimeAtStartOfDay();
        final DateTime roundedUserLocalTimeInUTC = new DateTime(userLocalDateTime.getMillis(), DateTimeZone.UTC);

        final Optional<SleepLabel> sleepLabelOptional = sleepLabelDAO.getByAccountAndDate(
                accessToken.accountId,
                roundedUserLocalTimeInUTC,
                timeZoneOffset
        );

        if(sleepLabelOptional.isPresent()){
            return Response.ok(sleepLabelOptional.get()).build();
        }else{
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    @Path("/save")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveLabel(@Valid final SleepLabel sleepLabel,
                              @Scope({OAuthScope.SLEEP_LABEL_WRITE}) final AccessToken accessToken){


        try{
            final DateTimeZone userLocalTimeZone = DateTimeZone.forOffsetMillis(sleepLabel.timeZoneOffset);
            final DateTime userLocalDateTime = new DateTime(sleepLabel.dateUTC.getMillis(), userLocalTimeZone);
            LOGGER.debug("Received sleep label for the night of {}", userLocalDateTime.toString("MM/dd/yyyy HH:mm:ss Z"));

            // Round on the user lcoal time instead of the UTC tme.
            final DateTime roundedUserLocalDateTime = userLocalDateTime.withTimeAtStartOfDay();
            final DateTime roundedUserLocalTimeInUTC = new DateTime(roundedUserLocalDateTime.getMillis(), DateTimeZone.UTC);

            this.sleepLabelDAO.inTransaction(TransactionIsolationLevel.SERIALIZABLE ,new Transaction<Void, SleepLabelDAO>() {

                @Override
                public Void inTransaction(SleepLabelDAO sleepLabelDAO, TransactionStatus transactionStatus)
                        throws Exception {

                    Optional<SleepLabel> sleepLabelOptional = sleepLabelDAO.getByAccountAndDate(
                            accessToken.accountId,
                            roundedUserLocalTimeInUTC,
                            sleepLabel.timeZoneOffset
                    );

                    if(sleepLabelOptional.isPresent()){
                        LOGGER.warn("Sleep label at {}, timezone {} found, label will be updated",
                                roundedUserLocalTimeInUTC,
                                sleepLabelOptional.get().timeZoneOffset);

                        sleepLabelDAO.updateBySleepLabelId(sleepLabelOptional.get().id,
                                sleepLabel.rating.getValue(),
                                sleepLabel.sleepTimeUTC,
                                sleepLabel.wakeUpTimeUTC);

                    }else{
                        sleepLabelDAO.insert(accessToken.accountId,
                                roundedUserLocalTimeInUTC,
                                sleepLabel.rating.getValue(),
                                sleepLabel.sleepTimeUTC,
                                sleepLabel.wakeUpTimeUTC,
                                sleepLabel.timeZoneOffset
                        );
                        LOGGER.debug("Sleep label at {}, timezone {} created, ",
                                roundedUserLocalTimeInUTC,
                                sleepLabel.timeZoneOffset);
                    }

                    return null; // What??

                }
            });

        }catch (UnableToExecuteStatementException ex){
            LOGGER.error(ex.getMessage());
            return Response.serverError().build();
        }

        return Response.ok().build();


    }
}

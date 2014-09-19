package com.hello.suripu.app.resources.v1;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by pangwu on 9/18/14.
 */
@Path("/v1/timezone")
public class TimeZoneResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeZoneResource.class);
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;

    public TimeZoneResource(final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB){
        this.timeZoneHistoryDAODynamoDB = timeZoneHistoryDAODynamoDB;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeZoneHistory setTimeZone(@Scope({OAuthScope.USER_BASIC}) final AccessToken token,
                                       final TimeZoneHistory timeZoneHistory){

        try {
            final Optional<TimeZoneHistory> timeZoneHistoryOptional = timeZoneHistoryDAODynamoDB.updateTimeZone(token.accountId,
                    timeZoneHistory.timeZoneId,
                    timeZoneHistory.offsetMillis
            );
            if (!timeZoneHistoryOptional.isPresent()) {
                LOGGER.error("account {} set timezone to id {}, offset {}, failed.",
                        token.accountId,
                        timeZoneHistory.timeZoneId,
                        timeZoneHistory.offsetMillis);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }


            return timeZoneHistoryOptional.get();
        }catch (AmazonServiceException awsException){
            LOGGER.error("Aws failed when account {} tries to set timezone.", token.accountId);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }

    }

}

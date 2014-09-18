package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by pangwu on 9/17/14.
 */
@Path("/v1/alarm")
public class AlarmResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmResource.class);
    private final AlarmDAODynamoDB alarmDAODynamoDB;

    public AlarmResource(final AlarmDAODynamoDB alarmDAODynamoDB){
        this.alarmDAODynamoDB = alarmDAODynamoDB;
    }

    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Alarm> getAlarms(@Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken token){
        final List<Alarm> alarms = alarmDAODynamoDB.getAlarms(token.accountId);
        return alarms;

    }


    @Timed
    @POST
    @Path("/{client_time_utc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void setAlarms(@Scope({OAuthScope.API_INTERNAL_DATA_WRITE}) final AccessToken token,
                          @PathParam("client_time_utc") long clientTime,
                          final List<Alarm> alarms){

        if(Math.abs(DateTime.now().getMillis() - clientTime) > DateTimeConstants.MILLIS_PER_MINUTE){
            LOGGER.error("account_id {} set alarm failed, client time too off.", token.accountId);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        final Set<Integer> alarmDays = new HashSet<Integer>();
        for(final Alarm alarm:alarms){
            for(final Integer dayOfWeek:alarm.dayOfWeek) {
                if (alarmDays.contains(dayOfWeek)) {
                    LOGGER.error("account id {} set alarm failed, two alarm in the same day.", token.accountId);
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
                } else {
                    alarmDays.add(dayOfWeek);
                }
            }
        }

        this.alarmDAODynamoDB.setAlarms(token.accountId, alarms);

    }
}

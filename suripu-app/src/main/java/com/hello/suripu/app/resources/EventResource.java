package com.hello.suripu.app.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.EventDAODynamoDB;
import com.hello.suripu.core.db.util.DateTimeFormatString;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 6/16/14.
 */
@Path("/events")
public class EventResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventResource.class);
    private final EventDAODynamoDB eventDAODynamoDB;

    public EventResource(final EventDAODynamoDB eventDAODynamoDB){
        this.eventDAODynamoDB = eventDAODynamoDB;
    }

    @Timed
    @GET
    @Path("/{target_date_string}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getEvents(@Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken token,
                                 @PathParam("target_date_string") final String targetDateString){

        final DateTime targetDate = DateTime.parse(targetDateString, DateTimeFormat.forPattern(DateTimeFormatString.FORMAT_TO_DAY));
        final List<Event> events = this.eventDAODynamoDB.getEventsForDate(token.accountId, targetDate);
        return events;
    }


    @Timed
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<Event>> getEvents(@Scope({OAuthScope.API_INTERNAL_DATA_READ}) final AccessToken accessToken,
                                              final List<String> dateStrings){
        final ArrayList<DateTime> targetDates = new ArrayList<DateTime>();
        for (final String dateString:dateStrings){
            targetDates.add(DateTime.parse(dateString, DateTimeFormat.forPattern(DateTimeFormatString.FORMAT_TO_DAY)));
        }

        final ImmutableMap<DateTime, ImmutableList<Event>> dateEventsMap = this.eventDAODynamoDB.getEventsForDates(accessToken.accountId, targetDates);
        final HashMap<String, List<Event>> results = new HashMap<String, List<Event>>();
        for(final DateTime targetDate:dateEventsMap.keySet()){
            results.put(targetDate.toString(DateTimeFormatString.FORMAT_TO_DAY), dateEventsMap.get(targetDate));
        }

        return results;

    }
}

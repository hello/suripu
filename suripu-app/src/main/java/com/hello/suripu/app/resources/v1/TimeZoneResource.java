package com.hello.suripu.app.resources.v1;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created by pangwu on 9/18/14.
 */
@Path("/v1/timezone")
public class TimeZoneResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeZoneResource.class);
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB;
    private final DeviceDAO deviceDAO;

    public TimeZoneResource(final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB,
                            final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB,
                            final DeviceDAO deviceDAO){
        this.timeZoneHistoryDAODynamoDB = timeZoneHistoryDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.mergedAlarmInfoDynamoDB = mergedAlarmInfoDynamoDB;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeZoneHistory setTimeZone(@Scope({OAuthScope.USER_BASIC}) final AccessToken token,
                                       final TimeZoneHistory timeZoneHistory){

        final List<DeviceAccountPair> deviceAccountMap = this.deviceDAO.getDeviceAccountMapFromAccountId(token.accountId);
        if(deviceAccountMap.size() == 0){
            LOGGER.error("User {} tires to write timezone without connected to a Morpheus.", token.accountId);

            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
        }

        TimeZoneHistory returnValue = null;
        for(final DeviceAccountPair deviceAccountPair:deviceAccountMap)
        {
            try {
                final AlarmInfo alarmInfo = new AlarmInfo(deviceAccountPair.externalDeviceId, token.accountId,
                        Optional.<List<Alarm>>absent(),
                        Optional.<RingTime>absent(),
                        Optional.of(DateTimeZone.forID(timeZoneHistory.timeZoneId)));

                this.mergedAlarmInfoDynamoDB.setInfo(alarmInfo);

                final Optional<TimeZoneHistory> timeZoneHistoryOptional = this.timeZoneHistoryDAODynamoDB.updateTimeZone(token.accountId,
                        timeZoneHistory.timeZoneId,
                        timeZoneHistory.offsetMillis
                );

                if (!timeZoneHistoryOptional.isPresent()) {
                    LOGGER.error("account {} set timezone history to id {}, offset {}, failed.",
                            token.accountId,
                            timeZoneHistory.timeZoneId,
                            timeZoneHistory.offsetMillis);
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
                }


                returnValue = new TimeZoneHistory(token.accountId,
                        alarmInfo.timeZone.get().getOffset(DateTime.now()),
                        alarmInfo.timeZone.get().getID());
            }catch (AmazonServiceException awsException){
                LOGGER.error("Aws failed when account {} tries to set timezone.", token.accountId);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
            }
        }

        return returnValue;


    }

}

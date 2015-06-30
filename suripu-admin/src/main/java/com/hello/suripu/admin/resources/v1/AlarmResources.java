package com.hello.suripu.admin.resources.v1;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.hello.suripu.admin.Util;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;


@Path("/v1/alarms")
public class AlarmResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmResources.class);
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final DeviceDAO deviceDAO;
    private final AccountDAO accountDAO;

    public AlarmResources(final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                          final DeviceDAO deviceDAO,
                          final AccountDAO accountDAO){
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.deviceDAO = deviceDAO;
        this.accountDAO = accountDAO;
    }

    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{email}")
    public List<Alarm> getAlarms(@Scope({OAuthScope.ADMINISTRATION_READ}) final AccessToken token,
                                 @PathParam("email") final String email ){

        final Optional<Long> accountIdOptional =  Util.getAccountIdByEmail(accountDAO, email);
        if (!accountIdOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "Account not found")).build());
        }

        final Long accountId = accountIdOptional.get();

        LOGGER.debug("Before getting device account map from account_id");
        final List<DeviceAccountPair> deviceAccountMap = this.deviceDAO.getSensesForAccountId(accountId);
        if(deviceAccountMap.size() == 0){
            LOGGER.error("User {} tries to retrieve alarm without paired with a Morpheus.", accountId);
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED).entity(
                    new JsonError(Response.Status.PRECONDITION_FAILED.getStatusCode(), "Please make sure your Sense is paired to your account before setting your alarm.")).build());
        }

        try {
            LOGGER.debug("Before getting device account map from account_id");
            final Optional<UserInfo> alarmInfoOptional = this.mergedUserInfoDynamoDB.getInfo(deviceAccountMap.get(0).externalDeviceId, accountId);
            LOGGER.debug("Fetched alarm info optional");

            if(!alarmInfoOptional.isPresent()){
                LOGGER.warn("Merge alarm info table doesn't have record for device {}, account {}.", deviceAccountMap.get(0).externalDeviceId, accountId);
                return Collections.emptyList();
            }


            final UserInfo userInfo = alarmInfoOptional.get();
            if(!userInfo.timeZone.isPresent()){
                LOGGER.error("User {} tries to get alarm without having a time zone.", accountId);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                        new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), "Please update your timezone and try again.")).build());
            }

            final DateTimeZone userTimeZone = userInfo.timeZone.get();
            final List<Alarm> smartAlarms = Alarm.Utils.disableExpiredNoneRepeatedAlarms(userInfo.alarmList, DateTime.now().getMillis(), userTimeZone);
            final Alarm.Utils.AlarmStatus status = Alarm.Utils.isValidAlarms(smartAlarms, DateTime.now(), userTimeZone);
            if(!status.equals(Alarm.Utils.AlarmStatus.OK)){
                LOGGER.error("Invalid alarm for user {} device {}", accountId, userInfo.deviceId);
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT).entity(
                        new JsonError(Response.Status.CONFLICT.getStatusCode(), "We could not save your changes, please try again.")).build());
            }

            return smartAlarms;
        }catch (AmazonServiceException awsException){
            LOGGER.error("Aws failed when user {} tries to get alarms.", accountId);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please try again.")).build());
        }
    }
}
package com.hello.suripu.app.resources.v1;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.exceptions.TooManyAlarmsException;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.AlarmUtils;
import com.hello.suripu.core.util.JsonError;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Created by pangwu on 9/17/14.
 */
@Path("/v1/alarms")
public class AlarmResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmResource.class);
    private final AlarmDAODynamoDB alarmDAODynamoDB;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final DeviceDAO deviceDAO;
    private final AmazonS3 amazonS3;

    public AlarmResource(final AlarmDAODynamoDB alarmDAODynamoDB,
                         final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                         final DeviceDAO deviceDAO,
                         final AmazonS3 amazonS3){
        this.alarmDAODynamoDB = alarmDAODynamoDB;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.deviceDAO = deviceDAO;
        this.amazonS3 = amazonS3;
    }

    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Alarm> getAlarms(@Scope({OAuthScope.ALARM_READ}) final AccessToken token){
        LOGGER.debug("Before getting device account map from account_id");
        final List<DeviceAccountPair> deviceAccountMap = this.deviceDAO.getSensesForAccountId(token.accountId);
        if(deviceAccountMap.size() == 0){
            LOGGER.error("User {} tries to retrieve alarm without paired with a Morpheus.", token.accountId);
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED).entity(
                    new JsonError(Response.Status.PRECONDITION_FAILED.getStatusCode(), "Please make sure your Sense is paired to your account before setting your alarm.")).build());
        }

        try {
            LOGGER.debug("Before getting device account map from account_id");
            final Optional<UserInfo> alarmInfoOptional = this.mergedUserInfoDynamoDB.getInfo(deviceAccountMap.get(0).externalDeviceId, token.accountId);
            LOGGER.debug("Fetched alarm info optional");

            if(!alarmInfoOptional.isPresent()){
                LOGGER.warn("Merge alarm info table doesn't have record for device {}, account {}.", deviceAccountMap.get(0).externalDeviceId, token.accountId);
//                throw new WebApplicationException(Response.Status.BAD_REQUEST);
                return Collections.emptyList();
            }


            final UserInfo userInfo = alarmInfoOptional.get();
            if(!userInfo.timeZone.isPresent()){
                LOGGER.error("User {} tries to get alarm without having a time zone.", token.accountId);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                        new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), "Please update your timezone and try again.")).build());
            }

            final DateTimeZone userTimeZone = userInfo.timeZone.get();
            final List<Alarm> smartAlarms = Alarm.Utils.disableExpiredNoneRepeatedAlarms(userInfo.alarmList, DateTime.now().getMillis(), userTimeZone);
            final Alarm.Utils.AlarmStatus status = Alarm.Utils.isValidAlarms(smartAlarms, DateTime.now(), userTimeZone);
            if(!status.equals(Alarm.Utils.AlarmStatus.OK)){
                LOGGER.error("Invalid alarm for user {} device {}", token.accountId, userInfo.deviceId);
                throw new WebApplicationException(Response.status(Response.Status.CONFLICT).entity(
                        new JsonError(Response.Status.CONFLICT.getStatusCode(), "We could not save your changes, please try again.")).build());
            }

            return smartAlarms;
        }catch (AmazonServiceException awsException){
            LOGGER.error("Aws failed when user {} tries to get alarms.", token.accountId);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please try again.")).build());
        }

    }


    @Timed
    @POST
    @Path("/{client_time_utc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Alarm> setAlarms(@Scope({OAuthScope.ALARM_WRITE}) final AccessToken token,
                          @PathParam("client_time_utc") long clientTime,
                          final List<Alarm> alarms){

        final DateTime now = DateTime.now();
        if(!AlarmUtils.isWithinReasonableBounds(now, clientTime)) {
            LOGGER.error("account_id {} set alarm failed, client time too off.( was {}, now is {}", token.accountId, clientTime, now);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                    new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), English.ERROR_CLOCK_OUT_OF_SYNC)).build()
            );
        }

        final List<DeviceAccountPair> deviceAccountMap = this.deviceDAO.getSensesForAccountId(token.accountId);
        if(deviceAccountMap.size() == 0){
            LOGGER.error("Account {} tries to set alarm without connected to a Morpheus.", token.accountId);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        // Only update alarms in the account that linked with the most recent sense.
        final DeviceAccountPair deviceAccountPair = deviceAccountMap.get(0);
        try {
            final Optional<UserInfo> alarmInfoOptional = this.mergedUserInfoDynamoDB.getInfo(deviceAccountPair.externalDeviceId, token.accountId);
            if(!alarmInfoOptional.isPresent()){
                LOGGER.warn("No merge info for user {}, device {}", token.accountId, deviceAccountPair.externalDeviceId);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }

            if(!alarmInfoOptional.get().timeZone.isPresent()){
                LOGGER.warn("No user timezone set for account {}, device {}, alarm set skipped.", deviceAccountPair.accountId, deviceAccountPair.externalDeviceId);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }

            final DateTimeZone timeZone = alarmInfoOptional.get().timeZone.get();
            final Alarm.Utils.AlarmStatus status = Alarm.Utils.isValidAlarms(alarms, DateTime.now(), timeZone);

            if(status.equals(Alarm.Utils.AlarmStatus.OK)) {
                if(!this.mergedUserInfoDynamoDB.setAlarms(deviceAccountPair.externalDeviceId, token.accountId,
                        alarmInfoOptional.get().lastUpdatedAt,
                        alarmInfoOptional.get().alarmList,
                        alarms,
                        alarmInfoOptional.get().timeZone.get())){
                    LOGGER.warn("Cannot update alarm, race condition for account: {}", token.accountId);
                    throw new WebApplicationException(Response.status(Response.Status.CONFLICT).entity(
                            new JsonError(Response.Status.CONFLICT.getStatusCode(),
                                    "Cannot update alarm, please refresh and try again.")).build());
                }
                this.alarmDAODynamoDB.setAlarms(token.accountId, alarms);
            }

            if(status.equals(Alarm.Utils.AlarmStatus.SMART_ALARM_ALREADY_SET)){
                LOGGER.error("Invalid alarm for account {}, device {}, alarm set skipped. Smart alarm already set.", deviceAccountPair.accountId, deviceAccountPair.externalDeviceId);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                        new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), "Currently, you can only set one Smart Alarm per day. You already have a Smart Alarm scheduled for this day.")).build());
            }


        }catch (AmazonServiceException awsException){
            LOGGER.error("Aws failed when user {} tries to get alarms.", token.accountId);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please try again.")).build());
        }catch (TooManyAlarmsException tooManyAlarmException){
            LOGGER.error("Account {} tries to set {} alarm, too many alarm", token.accountId, alarms.size());
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            String.format("You can not set more than %d alarms.", AlarmDAODynamoDB.MAX_ALARM_COUNT))).build());
        }

        return alarms;

    }


    @Timed
    @GET
    @Path("/sounds")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AlarmSound> getAlarmSounds(@Scope(OAuthScope.ALARM_READ) final AccessToken accessToken) {
        final List<AlarmSound> alarmSounds = Lists.newArrayList();


        // Note: this is the order in which they appear in the app.
        final List<SoundTuple> sounds = Lists.newArrayList(
                new SoundTuple("Dusk", 5),
                new SoundTuple("Pulse", 4),
                new SoundTuple("Lilt", 6),
                new SoundTuple("Bounce", 7),
                new SoundTuple("Celebration", 8),
                new SoundTuple("Milky Way", 9),
                new SoundTuple("Waves", 10),
                new SoundTuple("Lights", 11),
                new SoundTuple("Echo", 12),
                new SoundTuple("Drops", 13),
                new SoundTuple("Twinkle", 14),
                new SoundTuple("Silver", 15),
                new SoundTuple("Highlights", 16),
                new SoundTuple("Ripple", 17),
                new SoundTuple("Sway", 18)
        );

        for(final SoundTuple tuple: sounds) {
            final URL url = amazonS3.generatePresignedUrl("hello-audio", String.format("ringtones/%s.mp3", tuple.displayName), DateTime.now().plusWeeks(1).toDate());
            final AlarmSound sound = new AlarmSound(tuple.id, tuple.displayName, url.toExternalForm());
            alarmSounds.add(sound);
        }

        return alarmSounds;
    }

    private static class SoundTuple {
        public final String displayName;
        public final Integer id;

        public SoundTuple(final String displayName, final Integer id) {
            this.id = id;
            this.displayName = displayName;
        }
    }
}
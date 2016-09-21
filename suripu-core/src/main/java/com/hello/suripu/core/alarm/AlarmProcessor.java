package com.hello.suripu.core.alarm;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by ksg on 9/20/16
 */
public class AlarmProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmProcessor.class);

    private final AlarmDAODynamoDB alarmDAODynamoDB;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;

    public AlarmProcessor(final AlarmDAODynamoDB alarmDAODynamoDB, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
        this.alarmDAODynamoDB = alarmDAODynamoDB;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
    }

    public List<Alarm> getAlarms(final Long accountId,
                                 final String senseExternalId) throws InvalidTimezoneException {

        try {
            LOGGER.debug("action=get-alarm-info");
            final Optional<UserInfo> alarmInfoOptional = this.mergedUserInfoDynamoDB.getInfo(senseExternalId, accountId);
            if(!alarmInfoOptional.isPresent()){
                LOGGER.warn("warning=get-alarm-fail-missing-merge-info account_id={} sense_id={}", accountId, senseExternalId);
                return Collections.emptyList();
            }

            final UserInfo userInfo = alarmInfoOptional.get();
            if(!userInfo.timeZone.isPresent()){
                LOGGER.error("error=get-alarm-fail-missing-timezone account_id={}", accountId);
                throw new InvalidTimezoneException("Please update your timezone and try again.");
//                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
//                        new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), "Please update your timezone and try again.")).build());
            }

            final DateTimeZone userTimeZone = userInfo.timeZone.get();
            final List<Alarm> smartAlarms = Alarm.Utils.disableExpiredNoneRepeatedAlarms(userInfo.alarmList, DateTime.now().getMillis(), userTimeZone);

            final Alarm.Utils.AlarmStatus status = Alarm.Utils.isValidAlarms(smartAlarms, DateTime.now(), userTimeZone);
            if(!status.equals(Alarm.Utils.AlarmStatus.OK)) {
                LOGGER.error("error=invalid-alarm account_id={} sense_id={}", accountId, senseExternalId);
                // TODO: check with Tim about this exception message
                throw new InvalidAlarmException("We could not save your changes, please try again.");
//                throw new WebApplicationException(Response.status(Response.Status.CONFLICT).entity(
//                        new JsonError(Response.Status.CONFLICT.getStatusCode(), "We could not save your changes, please try again.")).build());
            }

            return smartAlarms;

        } catch (AmazonServiceException awsException){
            LOGGER.error("error=aws-service-exception-during-get-alarms account_id={}", accountId);
            throw new GetAlarmException("Please try again.");
//            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
//                    new JsonError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please try again.")).build());
        }
    }

    public void setAlarms(final Long accountId,
                          final String senseExternalId,
                          final List<Alarm> alarms)  throws
            InvalidUserException, InvalidTimezoneException,  AlarmConflictException, GetAlarmException, TooManyAlarmsException {

        // Only update alarms in the account that linked with the most recent sense.
        try {
            final Optional<UserInfo> alarmInfoOptional = this.mergedUserInfoDynamoDB.getInfo(senseExternalId, accountId);
            if(!alarmInfoOptional.isPresent()){
                LOGGER.warn("warning=no-merge-info account_id={} sense_id={}", accountId, senseExternalId);
                throw new InvalidUserException("no merge info");
            }

            if(!alarmInfoOptional.get().timeZone.isPresent()){
                LOGGER.warn("warning=no-user-timezone-set account_id={} sense_id={} action=alarm-set-skipped.", accountId, senseExternalId);
                throw new InvalidTimezoneException("no timezone");
                // throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }

            final DateTimeZone timeZone = alarmInfoOptional.get().timeZone.get();
            final Alarm.Utils.AlarmStatus status = Alarm.Utils.isValidAlarms(alarms, DateTime.now(), timeZone);

            if(status.equals(Alarm.Utils.AlarmStatus.OK)) {
                if(!this.mergedUserInfoDynamoDB.setAlarms(senseExternalId, accountId,
                        alarmInfoOptional.get().lastUpdatedAt,
                        alarmInfoOptional.get().alarmList,
                        alarms,
                        alarmInfoOptional.get().timeZone.get())){
                    LOGGER.warn("warning=cannot-update-alarm reason=race-condition account_id={}", accountId);
                    throw new AlarmConflictException("Cannot update alarm, please refresh and try again.");
                }
                this.alarmDAODynamoDB.setAlarms(accountId, alarms);
            }

            if(status.equals(Alarm.Utils.AlarmStatus.SMART_ALARM_ALREADY_SET)){
                LOGGER.error("error=smart-alarm-already-set account_id={} sense_id={} action=alarm-set-skipped..", accountId, senseExternalId);
                throw new DuplicateSmartAlarmException("Currently, you can only set one Smart Alarm per day. You already have a Smart Alarm scheduled for this day.");
            }

        } catch (AmazonServiceException awsException){
            LOGGER.error("error=aws-service-exception-during-get-alarms account_id={}", accountId);
            throw new GetAlarmException("Please try again.");

        } catch (TooManyAlarmsException tooManyAlarmException){
            LOGGER.error("error=too-many-alarms-set account_id={} alarm_size={}", accountId, alarms.size());
            throw new TooManyAlarmsException(String.format("You can not set more than %d alarms.", AlarmDAODynamoDB.MAX_ALARM_COUNT));
        }
    }
}

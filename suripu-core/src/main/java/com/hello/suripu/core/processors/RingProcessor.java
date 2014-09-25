package com.hello.suripu.core.processors;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.event.SleepCycleAlgorithm;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 9/24/14.
 */
public class RingProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(RingProcessor.class);

    public static class PipeDataSource implements DataSource<AmplitudeData> {

        private final List<AmplitudeData> motionAmplitudes;
        public PipeDataSource(final List<TrackerMotion> motions){
            this.motionAmplitudes = new LinkedList<AmplitudeData>();

            long lastTimestamp = 0;
            for(final TrackerMotion motion:motions){
                if(lastTimestamp != 0){
                    // If there is gap in data.
                    if(motion.timestamp - lastTimestamp > 2 * DateTimeConstants.MILLIS_PER_MINUTE){
                       long diff = motion.timestamp - lastTimestamp;
                       long insertCount = diff / DateTimeConstants.MILLIS_PER_MINUTE;
                        for(int i = 0; i < insertCount; i++){
                            this.motionAmplitudes.add(new AmplitudeData(lastTimestamp + (i+1) * DateTimeConstants.MILLIS_PER_MINUTE, 0, motion.offsetMillis));
                        }
                    }


                }

                this.motionAmplitudes.add(new AmplitudeData(motion.timestamp, motion.value == -1 ? 0 : motion.value, motion.offsetMillis));
                lastTimestamp = motion.timestamp;
            }
        }

        @Override
        public ImmutableList<AmplitudeData> getDataForDate(final DateTime day) {
            return ImmutableList.copyOf(this.motionAmplitudes);
        }
    }

    @Timed
    public static RingTime updateNextRingTime(final AlarmDAODynamoDB alarmDAODynamoDB,
                                          final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB,
                                          final RingTimeDAODynamoDB ringTimeDAODynamoDB,
                                          final DeviceDAO deviceDAO,
                                          final TrackerMotionDAO trackerMotionDAO,
                                          final String morpheusId,
                                          final DateTime currentTime,
                                          int smartAlarmProcessAheadInMinutes,
                                          int slidingWindowSizeInMinutes){
        final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(morpheusId);
        RingTime currentRingTime = ringTimeDAODynamoDB.getNextRingTime(morpheusId);

        final ArrayList<RingTime> ringTimes = new ArrayList<RingTime>();
        DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");

        for (final DeviceAccountPair pair : deviceAccountPairs) {
            try {
                // TODO: Warning, since we query dynamoDB based on user input, the user can generate a lot of
                // requests to break our bank(Assume that Dynamo DB never goes down).

                // Get the timezone for current user.
                final Optional<TimeZoneHistory> timeZoneHistoryOptional = timeZoneHistoryDAODynamoDB.getCurrentTimeZone(pair.accountId);
                if (timeZoneHistoryOptional.isPresent()) {
                    userTimeZone = DateTimeZone.forID(timeZoneHistoryOptional.get().timeZoneId);
                } else {
                    LOGGER.warn("Failed to get user timezone for account {}", pair.accountId);
                }
            } catch (AmazonServiceException awsException) {
                // I guess this endpoint should never bail out?
                LOGGER.error("AWS error when retrieving user timezone for account {}", pair.accountId);
            }


            try {
                // Get alarms templates for a user
                final List<Alarm> alarms = alarmDAODynamoDB.getAlarms(pair.accountId);

                // Based on current time, the user's alarm template & user's current timezone, compute
                // the next ring moment.

                final RingTime nextRingTime = Alarm.Utils.getNextRingTime(alarms, currentTime.getMillis(), userTimeZone);
                if (!nextRingTime.isEmpty()) {
                    ringTimes.add(nextRingTime);  // Add the alarm of this user to the list.
                } else {
                    LOGGER.debug("Alarm worker: No alarm set for account {}", pair.accountId);
                }


            } catch (AmazonServiceException awsException) {
                LOGGER.error("AWS error when retrieving alarm for account {}.", pair.accountId);
            }
        }

        // Now we loop over all the users, we get a list of ring time for all users.
        // Let's pick the nearest one and tell morpheus what is the next ring time.
        final RingTime[] shortedRingTime = ringTimes.toArray(new RingTime[0]);
        Arrays.sort(shortedRingTime, new Comparator<RingTime>() {
            @Override
            public int compare(final RingTime o1, final RingTime o2) {
                return Long.compare(o1.actualRingTimeUTC, o2.actualRingTimeUTC);
            }
        });

        RingTime ringTime = RingTime.createEmpty();

        if (shortedRingTime.length > 0) {
            final RingTime nextRingTime = shortedRingTime[0];

            if(currentRingTime.isEmpty()){
                // There is no current ring time. The next ring is the first alarm this device have.
                // At this time, the next ring is a regular alarm, not smart yet.
                currentRingTime = nextRingTime;  // Set current to next and try to trigger smart alarm computation.
                ringTimeDAODynamoDB.setNextRingTime(morpheusId, nextRingTime);
            }

            if(currentRingTime.equals(nextRingTime)) {
                if (currentRingTime.isSmart()) {
                    // Skip, don't need to do anything, wait for update after this alarm passed.
                    ringTime = currentRingTime;
                } else {

                    // Try to get smart alarm time.
                    // Check if the current time is N min before next ring.
                    final DateTime actualRingTimeLocal = new DateTime(nextRingTime.actualRingTimeUTC, userTimeZone);
                    if(currentTime.plusMinutes(smartAlarmProcessAheadInMinutes).isAfter(actualRingTimeLocal) ||
                            currentTime.plusMinutes(smartAlarmProcessAheadInMinutes).isEqual(actualRingTimeLocal)){
                        // Compute sleep cycles.

                        final Long[] smartAlarmRings = new Long[deviceAccountPairs.size()];
                        int index = 0;
                        // Loop through all the fucking users connected to this morpheus
                        for (final DeviceAccountPair pair : deviceAccountPairs) {
                            // Select data from NOW back to 8 hours before.

                            // Convert the dataCollectionTime to local time.
                            final DateTime dataCollectionTime = new DateTime(currentTime, userTimeZone);
                            // Convert the local data collection time to local UTC time, for select motion data.
                            final DateTime dataCollectionTimeLocalUTC = new DateTime(dataCollectionTime.getYear(),
                                    dataCollectionTime.getMonthOfYear(),
                                    dataCollectionTime.getDayOfMonth(),
                                    dataCollectionTime.getHourOfDay(),
                                    dataCollectionTime.getMinuteOfHour(),
                                    0,
                                    0,
                                    DateTimeZone.UTC);

                            // Get the end time for pill data select.
                            final DateTime selectStartTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);
                            final List<TrackerMotion> pillData = trackerMotionDAO.getBetweenLocalUTC(pair.accountId, selectStartTimeLocalUTC, dataCollectionTimeLocalUTC);

                            if(pillData.size() == 0){
                                LOGGER.info("The user {} have no motion data upload from {} to {}, in Local UTC, ring set to {}",
                                        pair.accountId, selectStartTimeLocalUTC, dataCollectionTime,
                                        new DateTime(nextRingTime.actualRingTimeUTC, userTimeZone));
                                smartAlarmRings[index] = nextRingTime.actualRingTimeUTC;
                            }else{
                                final PipeDataSource pipeDataSource = new PipeDataSource(pillData);
                                final SleepCycleAlgorithm sleepCycleAlgorithm = new SleepCycleAlgorithm(pipeDataSource, slidingWindowSizeInMinutes);
                                final List<Segment> sleepCycles = sleepCycleAlgorithm.getCycles(new DateTime(nextRingTime.expectedRingTimeUTC, userTimeZone));
                                final DateTime smartAlarmRingTimeUTC = sleepCycleAlgorithm.getSmartAlarmTimeUTC(sleepCycles, currentTime.getMillis(), nextRingTime.expectedRingTimeUTC);
                                LOGGER.info("User {} smartAlarm time is {}", pair.accountId, new DateTime(smartAlarmRingTimeUTC, userTimeZone));
                                smartAlarmRings[index] = smartAlarmRingTimeUTC.getMillis();
                            }

                            index++;
                        }
                        // Sort all the smart alarms from users.
                        Arrays.sort(smartAlarmRings, new Comparator<Long>() {
                            @Override
                            public int compare(Long o1, Long o2) {
                                return -Long.compare(o1, o2);
                            }
                        });

                        // Select the latest smart alarm, let the user sleep as long as possible.
                        long selectedSmartAlarmTime = smartAlarmRings[0];
                        ringTime = new RingTime(selectedSmartAlarmTime, nextRingTime.expectedRingTimeUTC, nextRingTime.soundIds);

                        LOGGER.info("Device {} ring time updated to {}", morpheusId, new DateTime(ringTime.actualRingTimeUTC, userTimeZone));
                        ringTimeDAODynamoDB.setNextRingTime(morpheusId, ringTime);
                    }else{
                        // Too early to compute smart alarm time. There are two cases:
                        // 1) Next ring time is already set at this moment, but a regular one.
                        // 2) this is the first ring for the user, already saved.
                        // No need to update.
                        ringTime = currentRingTime;
                    }

                }
            }else{
                // update ringTime.
                final DateTime nextRingTimeLocal = new DateTime(nextRingTime.actualRingTimeUTC, userTimeZone);
                LOGGER.info("Device {} ring time updated to {}", morpheusId, nextRingTimeLocal);
                ringTimeDAODynamoDB.setNextRingTime(morpheusId, nextRingTime);
                ringTime = nextRingTime;
            }
        }else{
            // Mark the user don't have alarm.
            ringTime = RingTime.createEmpty();
            ringTimeDAODynamoDB.setNextRingTime(morpheusId, ringTime);
        }

        return ringTime;
    }


    @Timed
    public static RingTime getNextRegularRingTime(final AlarmDAODynamoDB alarmDAODynamoDB,
                                              final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB,
                                              final DeviceDAO deviceDAO,
                                              final String morpheusId,
                                              final DateTime currentTime){
        final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(morpheusId);

        final ArrayList<RingTime> ringTimes = new ArrayList<RingTime>();
        DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");

        for (final DeviceAccountPair pair : deviceAccountPairs) {
            try {
                // TODO: Warning, since we query dynamoDB based on user input, the user can generate a lot of
                // requests to break our bank(Assume that Dynamo DB never goes down).

                // Get the timezone for current user.
                final Optional<TimeZoneHistory> timeZoneHistoryOptional = timeZoneHistoryDAODynamoDB.getCurrentTimeZone(pair.accountId);
                if (timeZoneHistoryOptional.isPresent()) {
                    userTimeZone = DateTimeZone.forID(timeZoneHistoryOptional.get().timeZoneId);
                } else {
                    LOGGER.warn("Failed to get user timezone for account {}", pair.accountId);
                }
            } catch (AmazonServiceException awsException) {
                // I guess this endpoint should never bail out?
                LOGGER.error("AWS error when retrieving user timezone for account {}", pair.accountId);
            }


            try {
                // Get alarms templates for a user
                final List<Alarm> alarms = alarmDAODynamoDB.getAlarms(pair.accountId);

                // Based on current time, the user's alarm template & user's current timezone, compute
                // the next ring moment.

                final RingTime nextRingTime = Alarm.Utils.getNextRingTime(alarms, currentTime.getMillis(), userTimeZone);
                if (!nextRingTime.isEmpty()) {
                    ringTimes.add(nextRingTime);  // Add the alarm of this user to the list.
                } else {
                    LOGGER.debug("Alarm worker: No alarm set for account {}", pair.accountId);
                }


            } catch (AmazonServiceException awsException) {
                LOGGER.error("AWS error when retrieving alarm for account {}.", pair.accountId);
            }
        }

        // Now we loop over all the users, we get a list of ring time for all users.
        // Let's pick the nearest one and tell morpheus what is the next ring time.
        final RingTime[] shortedRingTime = ringTimes.toArray(new RingTime[0]);
        Arrays.sort(shortedRingTime, new Comparator<RingTime>() {
            @Override
            public int compare(final RingTime o1, final RingTime o2) {
                return Long.compare(o1.actualRingTimeUTC, o2.actualRingTimeUTC);
            }
        });

        RingTime ringTime = RingTime.createEmpty();

        if (shortedRingTime.length > 0) {
            final RingTime nextRingTime = shortedRingTime[0];
            ringTime = nextRingTime;
        }

        return ringTime;
    }
}

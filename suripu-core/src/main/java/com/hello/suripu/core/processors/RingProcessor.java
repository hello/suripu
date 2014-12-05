package com.hello.suripu.core.processors;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.event.SleepCycleAlgorithm;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TrackerMotion;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
    public static RingTime updateNextRingTime(final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB,
                                          final RingTimeDAODynamoDB ringTimeDAODynamoDB,
                                          final TrackerMotionDAO trackerMotionDAO,
                                          final String morpheusId,
                                          final DateTime currentTime,
                                          final int smartAlarmProcessAheadInMinutes,
                                          final int slidingWindowSizeInMinutes,
                                          final float lightSleepThreshold,
                                          final RolloutClient feature){

        Optional<List<AlarmInfo>> alarmInfoListOptional = Optional.absent();

        try{
            final List<AlarmInfo> alarmInfoList = mergedAlarmInfoDynamoDB.getInfo(morpheusId);
            alarmInfoListOptional = Optional.of(alarmInfoList);
        }catch (Exception ex){
            LOGGER.error("Get alarm info list for device {} failed: {}, {}", morpheusId, ex.getMessage(), ex.getClass().toString());
            return RingTime.createEmpty();
        }

        final List<AlarmInfo> alarmInfoList = alarmInfoListOptional.get();
        final List<RingTime> ringTimes = new ArrayList<RingTime>();

        for(final AlarmInfo alarmInfo:alarmInfoList){

            if(!userHasValidAlarmInfo(alarmInfo)){
                continue;
            }


            final List<Alarm> alarms = alarmInfo.alarmList;

            RingTime nextRegularRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplates(alarms, currentTime.getMillis(), alarmInfo.timeZone.get());

            if(nextRegularRingTime.isEmpty()){
                LOGGER.debug("Alarm worker: No alarm set for account {}", alarmInfo.accountId);
                continue;
            }
            final RingTime currentRingTime = getRingTimeFromAlarmInfo(alarmInfo);

            if(feature != null) {
                if (feature.userFeatureActive(FeatureFlipper.SMART_ALARM, alarmInfo.accountId, Collections.<String>emptyList())) {
                    final RingTime nextRingTime = updateNextSmartRingTime(currentTime,
                            slidingWindowSizeInMinutes, lightSleepThreshold, smartAlarmProcessAheadInMinutes,
                            currentRingTime, nextRegularRingTime,
                            alarmInfo,
                            trackerMotionDAO, mergedAlarmInfoDynamoDB);

                    ringTimes.add(nextRingTime);
                } else {
                    LOGGER.info("Account {} not in smart alarm group.", alarmInfo.accountId);
                    ringTimes.add(nextRegularRingTime);
                }
            }else{
                final RingTime nextRingTime = updateNextSmartRingTime(currentTime,
                        slidingWindowSizeInMinutes, lightSleepThreshold, smartAlarmProcessAheadInMinutes,
                        currentRingTime, nextRegularRingTime,
                        alarmInfo,
                        trackerMotionDAO, mergedAlarmInfoDynamoDB);

                ringTimes.add(nextRingTime);
            }

        }

        final RingTime mostRecentRingTime = getMostRecentRingTimeFromList(ringTimes);

        // Optional, just for backup
        appendNextRingTimeToRingTimeHistory(morpheusId, mostRecentRingTime, ringTimeDAODynamoDB);
        return mostRecentRingTime;
    }


    public static RingTime updateNextSmartRingTime(final DateTime currentTime,
                                                   final int slidingWindowSizeInMinutes,
                                                   final float lightSleepThreshold,
                                                   final int smartAlarmProcessAheadInMinutes,
                                                   final RingTime currentRingTime, final RingTime nextRegularRingTime,
                                                   final AlarmInfo alarmInfo,
                                                   final TrackerMotionDAO trackerMotionDAO,
                                                   final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB){

        LOGGER.info("Updating smart alarm for device {}, account {}", alarmInfo.deviceId, alarmInfo.accountId);
        
        if (currentRingTime.equals(nextRegularRingTime) && currentRingTime.isSmart()) {
            LOGGER.debug("Smart alarm already set to {} for device {}, account {}.",
                    new DateTime(currentRingTime.actualRingTimeUTC, alarmInfo.timeZone.get()),
                    alarmInfo.deviceId,
                    alarmInfo.accountId);
            return currentRingTime;
        }

        // currentRingTime.equals(nextRingTime) && currentRingTime.isSmart == false // next regular alarm generated, no pill data and not yet ring
        // currentRingTime.equals(nextRingTime) == false && currentRingTime.isSmart == false  // out-date last regular alarm due to no pill data
        // currentRingTime.equals(nextRingTime) == false && currentRingTime.isSmart == true   // out-date last smart alarm

        RingTime nextRingTime;
        // let's see if it is time to trigger the smart alarm processing.


        // Try to get smart alarm time.
        // Check if the current time is N min before next ring.
        final DateTime nextRegularRingTimeLocal = new DateTime(nextRegularRingTime.expectedRingTimeUTC, alarmInfo.timeZone.get());
        if (shouldTriggerSmartAlarmProcessing(currentTime, nextRegularRingTimeLocal, smartAlarmProcessAheadInMinutes)) {
            // It is time to compute sleep cycles.
            nextRingTime = getNextSmartRingTime(alarmInfo.accountId,
                    currentTime, alarmInfo.timeZone.get(),
                    nextRegularRingTime,
                    slidingWindowSizeInMinutes, lightSleepThreshold,
                    trackerMotionDAO);

            LOGGER.info("Device {} smart ring time updated to {}", alarmInfo.deviceId, new DateTime(nextRingTime.actualRingTimeUTC, alarmInfo.timeZone.get()));
            mergedAlarmInfoDynamoDB.setRingTime(alarmInfo.deviceId, alarmInfo.accountId, nextRingTime);

        } else {
            // Too early to compute smart alarm time.
            nextRingTime = nextRegularRingTime;
            if(currentRingTime.expectedRingTimeUTC < nextRegularRingTime.expectedRingTimeUTC){
                LOGGER.info("Device {} regular ring time updated to {}", alarmInfo.deviceId, new DateTime(nextRingTime.actualRingTimeUTC, alarmInfo.timeZone.get()));
                mergedAlarmInfoDynamoDB.setRingTime(alarmInfo.deviceId, alarmInfo.accountId, nextRingTime);
            }else if(currentRingTime.expectedRingTimeUTC > nextRegularRingTime.expectedRingTimeUTC){
                LOGGER.warn("Invalid current ring time for device {} and account {}", alarmInfo.deviceId, alarmInfo.accountId);
            }
        }

        return nextRingTime;
    }

    public static RingTime getMostRecentRingTimeFromList(final List<RingTime> ringTimes){
        // Now we loop over all the users, we get a list of ring time for all users.
        // Let's pick the nearest one and tell morpheus what is the next ring time.
        final RingTime[] sortedRingTime = ringTimes.toArray(new RingTime[0]);
        Arrays.sort(sortedRingTime, new Comparator<RingTime>() {
            @Override
            public int compare(final RingTime o1, final RingTime o2) {
                return Long.compare(o1.actualRingTimeUTC, o2.actualRingTimeUTC);
            }
        });

        final RingTime result = sortedRingTime.length > 0 ? sortedRingTime[0] : RingTime.createEmpty();

        return result;
    }

    public static RingTime getRingTimeFromAlarmInfo(final AlarmInfo alarmInfo){
        if(alarmInfo.ringTime.isPresent()){
            return alarmInfo.ringTime.get();
        }

        return RingTime.createEmpty();

    }


    public static boolean userHasValidAlarmInfo(final AlarmInfo alarmInfo){
        if(alarmInfo.alarmList.isEmpty()) {
            LOGGER.trace("Skip alarm for user {} device {} because empty alarm list", alarmInfo.accountId, alarmInfo.deviceId);
            return false;
        }

        // Based on current time, the user's alarm template & user's current timezone, compute
        // the next ring moment.
        if(!alarmInfo.timeZone.isPresent()){
            LOGGER.warn("Timezone not set for user {} on device {}", alarmInfo.accountId, alarmInfo.deviceId);
            return false;
        }

        return true;
    }

    public static boolean appendNextRingTimeToRingTimeHistory(final String morpheusId, final RingTime nextRingTime,
                                                              final RingTimeDAODynamoDB ringTimeDAODynamoDB){

        try {
            final RingTime lastRingTimeFromHistory = ringTimeDAODynamoDB.getNextRingTime(morpheusId);
            if (lastRingTimeFromHistory.actualRingTimeUTC < nextRingTime.actualRingTimeUTC) {
                ringTimeDAODynamoDB.setNextRingTime(morpheusId, nextRingTime);  // Just for backing up the history.
                return true;
            }
        }catch (AmazonServiceException awsException){
            LOGGER.error("Append last ring time for device {} failed: {}", morpheusId, awsException.getMessage());
        }

        return false;
    }

    public static boolean shouldTriggerSmartAlarmProcessing(final DateTime now, final DateTime nextRegularRingTime, final int smartAlarmProcessAheadInMinutes){
        return now.plusMinutes(smartAlarmProcessAheadInMinutes).isAfter(nextRegularRingTime) ||
                now.plusMinutes(smartAlarmProcessAheadInMinutes).isEqual(nextRegularRingTime);
    }

    public static RingTime getNextSmartRingTime(final long accountId,
                                           final DateTime now, final DateTimeZone timeZone,
                                           final RingTime nextRegularRingTime,
                                           final int slidingWindowSizeInMinutes,
                                           final float lightSleepThreshold,
                                           final TrackerMotionDAO trackerMotionDAO){
        final DateTime dataCollectionTime = new DateTime(now, timeZone);
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
        final List<TrackerMotion> pillData = trackerMotionDAO.getBetweenLocalUTC(accountId, selectStartTimeLocalUTC, dataCollectionTimeLocalUTC);
        long nextRingTimeMillis;

        if (pillData.size() == 0) {
            LOGGER.info("The user {} have no motion data upload from {} to {}, in Local UTC, ring set to {}",
                    accountId,
                    selectStartTimeLocalUTC, dataCollectionTime,
                    new DateTime(nextRegularRingTime.expectedRingTimeUTC, timeZone));
            nextRingTimeMillis = nextRegularRingTime.expectedRingTimeUTC;
        } else {
            final PipeDataSource pipeDataSource = new PipeDataSource(pillData);
            final SleepCycleAlgorithm sleepCycleAlgorithm = new SleepCycleAlgorithm(pipeDataSource, slidingWindowSizeInMinutes);
            final List<Segment> sleepCycles = sleepCycleAlgorithm.getCycles(new DateTime(nextRegularRingTime.expectedRingTimeUTC, timeZone),
                    lightSleepThreshold);
            final DateTime smartAlarmRingTimeUTC = sleepCycleAlgorithm.getSmartAlarmTimeUTC(sleepCycles,
                    now.getMillis(),
                    nextRegularRingTime.expectedRingTimeUTC);
            LOGGER.info("User {} smartAlarm time is {}", accountId, new DateTime(smartAlarmRingTimeUTC, timeZone));
            nextRingTimeMillis = smartAlarmRingTimeUTC.getMillis();
        }

        return new RingTime(nextRingTimeMillis, nextRegularRingTime.expectedRingTimeUTC, nextRegularRingTime.soundIds);
    }


    @Timed
    public static RingTime getNextRegularRingTime(final List<AlarmInfo> alarmInfoList,
                                              final String morpheusId,
                                              final DateTime currentTime){

        final ArrayList<RingTime> ringTimes = new ArrayList<RingTime>();
        DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");
        final HashMap<Long, ArrayList<RingTime>> groupedRingTime = new HashMap<>();


        try {

            for (final AlarmInfo alarmInfo:alarmInfoList){
                if(!alarmInfo.alarmList.isEmpty()){
                    final List<Alarm> alarms = alarmInfo.alarmList;
                    final RingTime nextRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplates(alarms, currentTime.getMillis(), userTimeZone);

                    if (!nextRingTime.isEmpty()) {
                        ringTimes.add(nextRingTime);  // Add the alarm of this user to the list.
                        if(!groupedRingTime.containsKey(nextRingTime.expectedRingTimeUTC)){
                            groupedRingTime.put(nextRingTime.expectedRingTimeUTC, new ArrayList<RingTime>());
                        }

                        // Group alarms based on their alarm deadlines
                        // So we can know if users had set same alarms.
                        groupedRingTime.get(nextRingTime.expectedRingTimeUTC).add(nextRingTime);
                    } else {
                        LOGGER.debug("Alarm worker: No alarm set for account {}", alarmInfo.accountId);
                    }
                }
            }


        } catch (AmazonServiceException awsException) {
            LOGGER.error("AWS error when retrieving alarm for device {}.", morpheusId);
        }


        // Now we loop over all the users, we get a list of ring time for all users.
        // Let's pick the closest one to the current time.
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
            final HashSet<Long> soundIds = new HashSet<>();

            // Okay, now we have the closest ring time, let's check if multiple users
            // had set alarms at the same ring time.
            final List<RingTime> sameRingFromDifferentUsers = groupedRingTime.get(nextRingTime.expectedRingTimeUTC);
            for(final RingTime ring:sameRingFromDifferentUsers){
                for(long soundId:ring.soundIds){
                    soundIds.add(soundId);
                }
            }
            ringTime = new RingTime(nextRingTime.actualRingTimeUTC, nextRingTime.expectedRingTimeUTC, soundIds.toArray(new Long[0]));
        }

        return ringTime;
    }
}

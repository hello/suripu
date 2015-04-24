package com.hello.suripu.core.processors;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.event.SleepCycleAlgorithm;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.ScheduledRingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.util.TrackerMotionUtils;
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
    private final static int SMART_ALARM_MIN_DELAY_MILLIS = 10 * DateTimeConstants.MILLIS_PER_MINUTE;  // This must be >= 2 * max possible data upload interval

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
    public static RingTime updateAndReturnNextRingTimeForSense(final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                                               final ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB,
                                                               final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB,
                                                               final TrackerMotionDAO trackerMotionDAO,
                                                               final String morpheusId,
                                                               final DateTime currentTimeNotAligned,
                                                               final int smartAlarmProcessAheadInMinutes,
                                                               final int slidingWindowSizeInMinutes,
                                                               final float lightSleepThreshold,
                                                               final RolloutClient feature){

        Optional<List<UserInfo>> alarmInfoListOptional = Optional.absent();

        try{
            final List<UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(morpheusId);
            alarmInfoListOptional = Optional.of(userInfoList);
        }catch (Exception ex){
            LOGGER.error("Get alarm info list for device {} failed: {}, {}", morpheusId, ex.getMessage(), ex.getClass().toString());
            return RingTime.createEmpty();
        }

        final List<UserInfo> userInfoList = alarmInfoListOptional.get();
        final List<RingTime> ringTimes = new ArrayList<RingTime>();

        for(final UserInfo userInfo : userInfoList){

            if(!userHasValidAlarmInfo(userInfo)){
                continue;
            }


            final List<Alarm> alarms = userInfo.alarmList;
            final DateTime currentUserLocalTime = Alarm.Utils.alignToMinuteGranularity(currentTimeNotAligned.withZone(userInfo.timeZone.get()));

            RingTime nextRingTimeFromTemplate = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarms, currentUserLocalTime.getMillis(), userInfo.timeZone.get());

            if(nextRingTimeFromTemplate.isEmpty()){
                LOGGER.debug("Alarm worker: No alarm set for account {}", userInfo.accountId);
                continue;
            }
            final RingTime nextRingTimeFromWorker = getRingTimeFromAlarmInfo(userInfo);

            if(feature != null) {
                if (feature.userFeatureActive(FeatureFlipper.SMART_ALARM, userInfo.accountId, Collections.<String>emptyList())) {
                    final RingTime nextRingTime = updateAndReturnNextSmartRingTimeForUser(currentUserLocalTime,
                            slidingWindowSizeInMinutes, lightSleepThreshold, smartAlarmProcessAheadInMinutes,
                            nextRingTimeFromWorker, nextRingTimeFromTemplate,
                            userInfo,
                            trackerMotionDAO,
                            mergedUserInfoDynamoDB, smartAlarmLoggerDynamoDB,
                            feature);

                    if(!nextRingTime.isEmpty()) {
                        ringTimes.add(nextRingTime);
                    }
                } else {
                    LOGGER.info("Account {} not in smart alarm group.", userInfo.accountId);
                    ringTimes.add(nextRingTimeFromTemplate);
                }
            }else{
                final RingTime nextRingTime = updateAndReturnNextSmartRingTimeForUser(currentUserLocalTime,
                        slidingWindowSizeInMinutes, lightSleepThreshold, smartAlarmProcessAheadInMinutes,
                        nextRingTimeFromWorker, nextRingTimeFromTemplate,
                        userInfo,
                        trackerMotionDAO,
                        mergedUserInfoDynamoDB, smartAlarmLoggerDynamoDB,
                        feature);

                if(!nextRingTime.isEmpty()) {
                    ringTimes.add(nextRingTime);
                }
            }

        }

        final RingTime mostRecentRingTime = getMostRecentRingTimeFromList(ringTimes);

        // Optional, just for backup
        appendNextRingTimeToSenseRingTimeHistory(morpheusId, mostRecentRingTime, scheduledRingTimeHistoryDAODynamoDB);
        return mostRecentRingTime;
    }


    protected static boolean isGivenRingTimeFromNextSmartAlarm(final DateTime currentTimeAlignedToStartOfMinute,
                                                               final RingTime nextRingTimeFromWorker){
        final boolean isCurrentTimeAfterNextRingTime = currentTimeAlignedToStartOfMinute.isAfter(nextRingTimeFromWorker.actualRingTimeUTC) == false;
        final boolean isProcessedSmartAlarm = nextRingTimeFromWorker.processed();

        return isCurrentTimeAfterNextRingTime && isProcessedSmartAlarm;
    }

    protected static boolean isCurrentTimeBetweenActualRingTimeAndExpectedRingTime(final DateTime currentTimeAlignedToStartOfMinute,
                                                                                   final RingTime nextRingTimeFromWorker){
        return currentTimeAlignedToStartOfMinute.isAfter(nextRingTimeFromWorker.actualRingTimeUTC) &&
                nextRingTimeFromWorker.processed() &&
                currentTimeAlignedToStartOfMinute.isBefore(nextRingTimeFromWorker.expectedRingTimeUTC);
    }

    protected static boolean hasSufficientTimeToApplyProgressiveSmartAlarm(final DateTime currentTimeAlignedToStartOfMinute,
                                                                           final RingTime nextRingTimeFromWorker,
                                                                           final int smartAlarmProcessAheadInMinutes){
        if(!nextRingTimeFromWorker.fromSmartAlarm || !nextRingTimeFromWorker.processed()){
            return false;
        }

        final boolean isNextSmartAlarmWithinProcessRange =  currentTimeAlignedToStartOfMinute
                .plusMinutes(smartAlarmProcessAheadInMinutes)
                .isBefore(nextRingTimeFromWorker.actualRingTimeUTC) == false;
        final boolean notTooCloseToRingTime = currentTimeAlignedToStartOfMinute.plusMillis(SMART_ALARM_MIN_DELAY_MILLIS).isBefore(nextRingTimeFromWorker.actualRingTimeUTC);
        return isNextSmartAlarmWithinProcessRange && notTooCloseToRingTime;
    }

    protected static Optional<RingTime> getProgressiveRingTime(final long accountId,
                                                               final DateTime nowAlignedToStartOfMinute,
                                                               final RingTime nextRingTimeFromWorker,
                                                               final TrackerMotionDAO trackerMotionDAO){
        final DateTime dataCollectionBeginTime = nowAlignedToStartOfMinute.minusMinutes(5);

        final List<TrackerMotion> motionFromLast5Minutes = trackerMotionDAO.getBetween(accountId,
                dataCollectionBeginTime, nowAlignedToStartOfMinute.plusMinutes(1));

        if(motionFromLast5Minutes.size() == 0){
            return Optional.absent();
        }
        final List<AmplitudeData> amplitudeData = TrackerMotionUtils.trackerMotionToAmplitudeData(motionFromLast5Minutes);
        final List<AmplitudeData> kickOffCounts = TrackerMotionUtils.trackerMotionToKickOffCounts(motionFromLast5Minutes);
        if(SleepCycleAlgorithm.isUserAwakeInGivenDataSpan(amplitudeData, kickOffCounts)){
            final RingTime progressiveRingTime = new RingTime(nowAlignedToStartOfMinute.plusMinutes(3).getMillis(),
                    nextRingTimeFromWorker.expectedRingTimeUTC,
                    nextRingTimeFromWorker.soundIds,
                    true);
            return Optional.of(progressiveRingTime);
        }
        return Optional.absent();
    }


    public static RingTime updateAndReturnNextSmartRingTimeForUser(final DateTime currentTimeAlignedToStartOfMinute,
                                                                   final int slidingWindowSizeInMinutes,
                                                                   final float lightSleepThreshold,
                                                                   final int smartAlarmProcessAheadInMinutes,
                                                                   final RingTime nextRingTimeFromWorker,
                                                                   final RingTime nextRingTimeFromTemplate,
                                                                   final UserInfo userInfo,
                                                                   final TrackerMotionDAO trackerMotionDAO,
                                                                   final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                                                                   final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB,
                                                                   final RolloutClient feature){

        LOGGER.info("Updating smart alarm for device {}, account {}", userInfo.deviceId, userInfo.accountId);
        // smart alarm computed, but not yet proceed to the actual ring time.
        if (isGivenRingTimeFromNextSmartAlarm(currentTimeAlignedToStartOfMinute, nextRingTimeFromWorker)) {
            if((feature == null || feature.userFeatureActive(FeatureFlipper.PROGRESSIVE_SMART_ALARM, userInfo.accountId, Collections.EMPTY_LIST)) &&
                    hasSufficientTimeToApplyProgressiveSmartAlarm(currentTimeAlignedToStartOfMinute, nextRingTimeFromWorker, smartAlarmProcessAheadInMinutes)){

                final Optional<RingTime> progressiveRingTimeOptional = getProgressiveRingTime(userInfo.accountId,
                        currentTimeAlignedToStartOfMinute,
                        nextRingTimeFromWorker,
                        trackerMotionDAO);
                if(progressiveRingTimeOptional.isPresent()){
                    mergedUserInfoDynamoDB.setRingTime(userInfo.deviceId, userInfo.accountId, progressiveRingTimeOptional.get());
                    smartAlarmLoggerDynamoDB.log(userInfo.accountId, new DateTime(0, DateTimeZone.UTC),
                            currentTimeAlignedToStartOfMinute.withZone(userInfo.timeZone.get()),
                            new DateTime(nextRingTimeFromWorker.actualRingTimeUTC, userInfo.timeZone.get()),
                            new DateTime(progressiveRingTimeOptional.get().expectedRingTimeUTC, userInfo.timeZone.get()),
                            Optional.of(new DateTime(progressiveRingTimeOptional.get().actualRingTimeUTC, userInfo.timeZone.get())));
                    LOGGER.info("Reset smart alarm with updated progressive smart alarm, original ring time {}, updated ring time {}",
                            new DateTime(nextRingTimeFromWorker.actualRingTimeUTC, userInfo.timeZone.get()),
                            new DateTime(progressiveRingTimeOptional.get().actualRingTimeUTC, userInfo.timeZone.get()));
                    return progressiveRingTimeOptional.get();
                }

            }

            LOGGER.debug("{} smart alarm already set to {} for device {}, account {}.",
                    currentTimeAlignedToStartOfMinute.withZone(userInfo.timeZone.get()),
                    new DateTime(nextRingTimeFromWorker.actualRingTimeUTC, userInfo.timeZone.get()),
                    userInfo.deviceId,
                    userInfo.accountId);
            return nextRingTimeFromWorker;
        }

        // previous ring time from worker expired, next alarm is a non-smart alarm, should use none-smart next ring time.
        if(!nextRingTimeFromTemplate.fromSmartAlarm){
            mergedUserInfoDynamoDB.setRingTime(userInfo.deviceId, userInfo.accountId, nextRingTimeFromTemplate);
            LOGGER.info("Device {} ring time updated to {}", userInfo.deviceId,
                    new DateTime(nextRingTimeFromTemplate.actualRingTimeUTC, userInfo.timeZone.get()));
            return nextRingTimeFromTemplate;
        }

        // The previous smart alarm is expired, but not yet pass the expected ring time.
        // We should return RingTime.empty because we don't want the alarm ring again during
        // this period.
        // We CANNOT just simply return nextRingTimeFromTemplate because they might be the same.
        if (isCurrentTimeBetweenActualRingTimeAndExpectedRingTime(currentTimeAlignedToStartOfMinute, nextRingTimeFromWorker)) {
            LOGGER.debug("{} smart alarm {} expired for device {}, account {}. Next alarm {}",
                    currentTimeAlignedToStartOfMinute.withZone(userInfo.timeZone.get()),
                    new DateTime(nextRingTimeFromWorker.actualRingTimeUTC, userInfo.timeZone.get()),
                    userInfo.deviceId,
                    userInfo.accountId,
                    new DateTime(nextRingTimeFromTemplate.actualRingTimeUTC, userInfo.timeZone.get()));
            if(nextRingTimeFromTemplate.equals(nextRingTimeFromWorker)) {
                // DO NOT write this into merge user info table, it will mess up
                // the states!
                return RingTime.createEmpty();  // Let the alarm stay quiet
            }

            // If the user update his/her alarm during this period, return the updated one from template
            return nextRingTimeFromTemplate;
        }

        // previous ring time from worker expired, next alarm is smart alarm, check if need to process next smart ring time.

        // currentRingTime.equals(nextRingTime) && currentRingTime.isSmart == false // next regular alarm generated, no pill data and not yet ring
        // currentRingTime.equals(nextRingTime) == false && currentRingTime.isSmart == false  // out-date last regular alarm due to no pill data
        // currentRingTime.equals(nextRingTime) == false && currentRingTime.isSmart == true   // out-date last smart alarm

        RingTime nextRingTime;
        // let's see if it is time to trigger the smart alarm processing.

        LOGGER.debug("{} worker alarm {} for device {}, account {}. Next template alarm {}",
                currentTimeAlignedToStartOfMinute.withZone(userInfo.timeZone.get()),
                new DateTime(nextRingTimeFromWorker.actualRingTimeUTC, userInfo.timeZone.get()),
                userInfo.deviceId,
                userInfo.accountId,
                new DateTime(nextRingTimeFromTemplate.actualRingTimeUTC, userInfo.timeZone.get()));
        // Try to get smart alarm time.
        // Check if the current time is N min before next ring.
        final DateTime nextRegularRingTimeLocal = new DateTime(nextRingTimeFromTemplate.expectedRingTimeUTC, userInfo.timeZone.get());
        if (shouldTriggerSmartAlarmProcessing(currentTimeAlignedToStartOfMinute, nextRegularRingTimeLocal, smartAlarmProcessAheadInMinutes)) {
            // It is time to compute sleep cycles.
            nextRingTime = processNextSmartRingTimeForUser(userInfo.accountId,
                    currentTimeAlignedToStartOfMinute,
                    currentTimeAlignedToStartOfMinute.plusMillis(SMART_ALARM_MIN_DELAY_MILLIS),
                    userInfo.timeZone.get(),
                    nextRingTimeFromTemplate,
                    slidingWindowSizeInMinutes, lightSleepThreshold,
                    trackerMotionDAO,
                    smartAlarmLoggerDynamoDB,
                    feature);

            LOGGER.info("Device {} smart ring time updated to {}", userInfo.deviceId, new DateTime(nextRingTime.actualRingTimeUTC, userInfo.timeZone.get()));
            mergedUserInfoDynamoDB.setRingTime(userInfo.deviceId, userInfo.accountId, nextRingTime);

        } else {
            // Too early to compute smart alarm time.
            nextRingTime = nextRingTimeFromTemplate;
            if(nextRingTimeFromWorker.expectedRingTimeUTC < nextRingTimeFromTemplate.expectedRingTimeUTC){
                LOGGER.info("Device {} regular ring time updated to {}", userInfo.deviceId, new DateTime(nextRingTime.actualRingTimeUTC, userInfo.timeZone.get()));
                mergedUserInfoDynamoDB.setRingTime(userInfo.deviceId, userInfo.accountId, nextRingTime);
            }else if(nextRingTimeFromWorker.expectedRingTimeUTC > nextRingTimeFromTemplate.expectedRingTimeUTC){
                LOGGER.warn("Invalid current ring time for device {} and account {}", userInfo.deviceId, userInfo.accountId);
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

    public static RingTime getRingTimeFromAlarmInfo(final UserInfo userInfo){
        if(userInfo.ringTime.isPresent()){
            return userInfo.ringTime.get();
        }

        return RingTime.createEmpty();

    }


    public static boolean userHasValidAlarmInfo(final UserInfo userInfo){
        if(userInfo.alarmList.isEmpty()) {
            LOGGER.trace("Skip alarm for user {} device {} because empty alarm list", userInfo.accountId, userInfo.deviceId);
            return false;
        }

        // Based on current time, the user's alarm template & user's current timezone, compute
        // the next ring moment.
        if(!userInfo.timeZone.isPresent()){
            LOGGER.warn("Timezone not set for user {} on device {}", userInfo.accountId, userInfo.deviceId);
            return false;
        }

        return true;
    }

    public static boolean appendNextRingTimeToSenseRingTimeHistory(final String morpheusId, final RingTime nextRingTime,
                                                                   final ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB){

        try {
            final RingTime lastRingTimeFromHistory = scheduledRingTimeHistoryDAODynamoDB.getNextRingTime(morpheusId);
            if (lastRingTimeFromHistory.actualRingTimeUTC != nextRingTime.actualRingTimeUTC) {
                scheduledRingTimeHistoryDAODynamoDB.setNextRingTime(morpheusId, nextRingTime);  // Just for backing up the history.
                return true;
            }
        }catch (AmazonServiceException awsException){
            LOGGER.error("Append last ring time for device {} failed: {}", morpheusId, awsException.getMessage());
        }

        return false;
    }

    public static boolean shouldTriggerSmartAlarmProcessing(final DateTime now, final DateTime nextRegularRingTime, final int smartAlarmProcessAheadInMinutes){
        return (!now.plusMinutes(smartAlarmProcessAheadInMinutes).isBefore(nextRegularRingTime)) &&
                (!now.isAfter(nextRegularRingTime.minusMinutes(5)));  // Don't process smart alarm when it is too close to expected ring time
    }

    public static RingTime processNextSmartRingTimeForUser(final long accountId,
                                                           final DateTime now,
                                                           final DateTime minRingTime,
                                                           final DateTimeZone timeZone,
                                                           final RingTime nextRegularRingTime,
                                                           final int slidingWindowSizeInMinutes,
                                                           final float lightSleepThreshold,
                                                           final TrackerMotionDAO trackerMotionDAO,
                                                           final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB,
                                                           final RolloutClient feature){
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
                    minRingTime.getMillis(),
                    nextRegularRingTime.expectedRingTimeUTC);

            if(feature != null && feature.userFeatureActive(FeatureFlipper.SMART_ALARM_LOGGING, accountId, Collections.EMPTY_LIST)){
                DateTime lastCycleEnds = new DateTime(0, DateTimeZone.UTC);
                if(sleepCycles.size() > 0){
                    lastCycleEnds = new DateTime(sleepCycles.get(sleepCycles.size() - 1).getEndTimestamp(), timeZone);
                }
                smartAlarmLoggerDynamoDB.log(accountId, lastCycleEnds, DateTime.now().withZone(timeZone),
                        smartAlarmRingTimeUTC.withZone(timeZone),
                        new DateTime(nextRegularRingTime.expectedRingTimeUTC, timeZone),
                        Optional.<DateTime>absent());
            }
            LOGGER.info("User {} smartAlarm time is {}", accountId, new DateTime(smartAlarmRingTimeUTC, timeZone));
            nextRingTimeMillis = smartAlarmRingTimeUTC.getMillis();
        }

        return new RingTime(nextRingTimeMillis, nextRegularRingTime.expectedRingTimeUTC, nextRegularRingTime.soundIds, nextRegularRingTime.fromSmartAlarm);
    }


    @Timed
    public static RingTime getNextRingTimeForSense(final String deviceId,
                                                   final List<UserInfo> userInfoFromThatDevice,
                                                   final DateTime nowUnalignedByMinute){
        RingTime nextRingTimeFromWorker = RingTime.createEmpty();
        RingTime nextRingTime = RingTime.createEmpty();
        Optional<DateTimeZone> userTimeZoneOptional = Optional.absent();

        //// Start: Try get the user time zone and ring time generated by smart alarm worker /////////
        for(final UserInfo userInfo : userInfoFromThatDevice){

            if(!userInfo.deviceId.equals(deviceId)){
                LOGGER.warn("alarm info list contains data not from device {}, got {}", deviceId, userInfo.deviceId);
                continue;
            }

            if(userInfo.timeZone.isPresent()){
                userTimeZoneOptional = Optional.of(userInfo.timeZone.get());
            }else{
                LOGGER.warn("User {} on device {} time zone not set.", userInfo.accountId, userInfo.deviceId);
                continue;
            }

            if(!userInfo.ringTime.isPresent()) {
                continue;
            }

            if(userInfo.ringTime.get().isEmpty()){
                continue;
            }

            if(nextRingTimeFromWorker.isEmpty()){
                nextRingTimeFromWorker = userInfo.ringTime.get();
                continue;
            }

            if(userInfo.ringTime.get().actualRingTimeUTC < nextRingTimeFromWorker.actualRingTimeUTC) {
                nextRingTimeFromWorker = userInfo.ringTime.get();
            }
        }

        if(!userTimeZoneOptional.isPresent()){  // No user timezone set, bail out.
            return RingTime.createEmpty();

        }
        //////End: Try get the user time zone and ring time generated by smart alarm worker /////////


        //// Start: Compute next ring time on-the-fly based on alarm templates, just in case the smart alarm worker dead /////
        Optional<RingTime> nextRegularRingTimeOptional = Optional.absent();

        try {
            nextRegularRingTimeOptional = Optional.of(RingProcessor.getNextRingTimeFromAlarmTemplateForSense(userInfoFromThatDevice,
                    deviceId,
                    Alarm.Utils.alignToMinuteGranularity(nowUnalignedByMinute.withZone(userTimeZoneOptional.get()))));
        }catch (Exception ex){
            LOGGER.error("Get next regular ring time for device {} failed: {}", deviceId, ex.getMessage());
        }
        //// End: Compute next ring time on the fly based on alarm template, just in case the smart alarm worker dead /////


        //// Start: Decide which ring time to be used: on-the-fly or the one generated by smart alarm worker? //////////
        if(nextRegularRingTimeOptional.isPresent()) {
            // By default, we use the on-the-fly one, because alarm worker may die.
            final RingTime nextRingTimeFromTemplate = nextRegularRingTimeOptional.get();
            final DateTime now = Alarm.Utils.alignToMinuteGranularity(nowUnalignedByMinute.withZone(userTimeZoneOptional.get()));

            if(!nextRingTimeFromTemplate.isEmpty()){
                if (nextRingTimeFromTemplate.expectedRingTimeUTC == nextRingTimeFromWorker.expectedRingTimeUTC) {
                    // on-the-fly and ring from worker are from the same alarm, use the one from worker
                    // since it is "smart"
                    if(now.isAfter(nextRingTimeFromWorker.actualRingTimeUTC)){
                        // The smart alarm already took off, do not ring twice!
                        nextRingTime = RingTime.createEmpty();
                    }else {
                        nextRingTime = nextRingTimeFromWorker;
                    }
                }

                if (nextRingTimeFromTemplate.expectedRingTimeUTC > nextRingTimeFromWorker.expectedRingTimeUTC) {
                    // We are in the intermediate time gap when:
                    // 1) The last ring generated by alarm worker has been fired, but
                    // 2) the alarm worker not yet receive the next data to trigger processing the next ring.
                    // 3) This is the data to trigger the worker to process the next ring (If it is still alive).
                    //
                    // Use the generated on-the-fly ring time.
                    LOGGER.debug("Ring time in merge table for device {} needs to update.", deviceId);
                    nextRingTime = nextRingTimeFromTemplate;
                }

                if(nextRingTimeFromTemplate.expectedRingTimeUTC < nextRingTimeFromWorker.expectedRingTimeUTC &&
                        now.isAfter(nextRingTimeFromTemplate.actualRingTimeUTC) == false){
                    // We are in the intermediate time gap when:
                    // 1) The last smart ring generated by alarm worker has been fired, but
                    // 2) the alarm worker not yet receive the next data, or current time not in processing window
                    //    to trigger process the next smart ring.
                    // 3) The user UPDATED the alarm list in this gap period.
                    //
                    // Use the generated on-the-fly ring time.
                    LOGGER.info("User update alarm when his/her is in smart alarm gap. Ring time in merge table for device {} needs to update.",
                            deviceId);
                    nextRingTime = nextRingTimeFromTemplate;
                }
            }

            LOGGER.debug("{} next ring time: {}", deviceId, new DateTime(nextRingTime.actualRingTimeUTC, userTimeZoneOptional.get()));


        }



        return nextRingTime;
    }

    @Timed
    public static RingTime getNextRingTimeFromAlarmTemplateForSense(final List<UserInfo> userInfoList,
                                                                    final String senseId,
                                                                    final DateTime currentTime){

        final ArrayList<RingTime> ringTimes = new ArrayList<RingTime>();
        final HashMap<Long, ArrayList<RingTime>> groupedRingTime = new HashMap<>();

        try {

            for (final UserInfo userInfo : userInfoList){
                if(!userInfo.timeZone.isPresent()){
                    LOGGER.error("No timezone set for device {} account {}, get regular ring time failed.", userInfo.deviceId, userInfo.accountId);
                    continue;
                }
                if(!userInfo.alarmList.isEmpty()){
                    final List<Alarm> alarms = userInfo.alarmList;
                    final RingTime nextRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarms, currentTime.getMillis(), userInfo.timeZone.get());

                    if (!nextRingTime.isEmpty()) {
                        ringTimes.add(nextRingTime);  // Add the alarm of this user to the list.
                        if(!groupedRingTime.containsKey(nextRingTime.expectedRingTimeUTC)){
                            groupedRingTime.put(nextRingTime.expectedRingTimeUTC, new ArrayList<RingTime>());
                        }

                        // Group alarms based on their alarm deadlines
                        // So we can know if users had set same alarms.
                        groupedRingTime.get(nextRingTime.expectedRingTimeUTC).add(nextRingTime);
                    } else {
                        LOGGER.debug("Alarm worker: No alarm set for account {}", userInfo.accountId);
                    }
                }
            }


        } catch (AmazonServiceException awsException) {
            LOGGER.error("AWS error when retrieving alarm for device {}.", senseId);
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
            ringTime = new RingTime(nextRingTime.actualRingTimeUTC, nextRingTime.expectedRingTimeUTC, soundIds.toArray(new Long[0]), nextRingTime.fromSmartAlarm);
        }

        return ringTime;
    }
}

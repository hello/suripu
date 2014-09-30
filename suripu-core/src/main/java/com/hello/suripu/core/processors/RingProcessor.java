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
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.RingTime;
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
                                          final float lightSleepThreshold){

        final List<AlarmInfo> alarmInfoList = mergedAlarmInfoDynamoDB.getInfo(morpheusId);

        final List<RingTime> ringTimes = new ArrayList<RingTime>();

        for(final AlarmInfo alarmInfo:alarmInfoList){
            final RingTime currentRingTime = alarmInfo.ringTime.isPresent() ? alarmInfo.ringTime.get() : RingTime.createEmpty();

            if(alarmInfo.alarmList.isPresent()) {
                final List<Alarm> alarms = alarmInfo.alarmList.get();

                // Based on current time, the user's alarm template & user's current timezone, compute
                // the next ring moment.
                if(!alarmInfo.timeZone.isPresent()){
                    LOGGER.warn("Timezone not set for user {} on device {}", alarmInfo.accountId, alarmInfo.deviceId);
                }else {

                    RingTime nextRingTime = Alarm.Utils.getNextRingTime(alarms, currentTime.getMillis(), alarmInfo.timeZone.get());

                    if(nextRingTime.isEmpty()){
                        LOGGER.debug("Alarm worker: No alarm set for account {}", alarmInfo.accountId);
                        continue;
                    }

                    if(currentRingTime.isEmpty()){
                        // There is no current ring time. The next ring is the first alarm this device have.
                        // At this time, the next ring is a regular alarm, not smart yet.

                        mergedAlarmInfoDynamoDB.setInfo(new AlarmInfo(morpheusId, alarmInfo.accountId,
                                Optional.<List<Alarm>>absent(),
                                Optional.of(nextRingTime),
                                Optional.<DateTimeZone>absent()));

                    } else {
                        if (currentRingTime.equals(nextRingTime)) {
                            // The next alarm is already generated.
                            // let's see if it is time to trigger the smart alarm processing.

                            if (!currentRingTime.isSmart()) {

                                // Try to get smart alarm time.
                                // Check if the current time is N min before next ring.
                                final DateTime actualRingTimeLocal = new DateTime(nextRingTime.actualRingTimeUTC, alarmInfo.timeZone.get());
                                if (currentTime.plusMinutes(smartAlarmProcessAheadInMinutes).isAfter(actualRingTimeLocal) ||
                                        currentTime.plusMinutes(smartAlarmProcessAheadInMinutes).isEqual(actualRingTimeLocal)) {
                                    // It is time to compute sleep cycles.

                                    final DateTime dataCollectionTime = new DateTime(currentTime, alarmInfo.timeZone.get());
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
                                    final List<TrackerMotion> pillData = trackerMotionDAO.getBetweenLocalUTC(alarmInfo.accountId, selectStartTimeLocalUTC, dataCollectionTimeLocalUTC);
                                    long selectedSmartAlarmTime = currentRingTime.expectedRingTimeUTC;

                                    if (pillData.size() == 0) {
                                        LOGGER.info("The user {} have no motion data upload from {} to {}, in Local UTC, ring set to {}",
                                                alarmInfo.accountId,
                                                selectStartTimeLocalUTC, dataCollectionTime,
                                                new DateTime(nextRingTime.actualRingTimeUTC, alarmInfo.timeZone.get()));
                                        selectedSmartAlarmTime = nextRingTime.actualRingTimeUTC;
                                    } else {
                                        final PipeDataSource pipeDataSource = new PipeDataSource(pillData);
                                        final SleepCycleAlgorithm sleepCycleAlgorithm = new SleepCycleAlgorithm(pipeDataSource, slidingWindowSizeInMinutes);
                                        final List<Segment> sleepCycles = sleepCycleAlgorithm.getCycles(new DateTime(nextRingTime.expectedRingTimeUTC, alarmInfo.timeZone.get()),
                                                lightSleepThreshold);
                                        final DateTime smartAlarmRingTimeUTC = sleepCycleAlgorithm.getSmartAlarmTimeUTC(sleepCycles,
                                                currentTime.getMillis(),
                                                nextRingTime.expectedRingTimeUTC);
                                        LOGGER.info("User {} smartAlarm time is {}", alarmInfo.accountId, new DateTime(smartAlarmRingTimeUTC, alarmInfo.timeZone.get()));
                                        selectedSmartAlarmTime = smartAlarmRingTimeUTC.getMillis();
                                    }

                                    final RingTime smartAlarmRingTime = new RingTime(selectedSmartAlarmTime, nextRingTime.expectedRingTimeUTC, nextRingTime.soundIds);

                                    LOGGER.info("Device {} ring time updated to {}", morpheusId, new DateTime(smartAlarmRingTime.actualRingTimeUTC, alarmInfo.timeZone.get()));
                                    mergedAlarmInfoDynamoDB.setInfo(new AlarmInfo(alarmInfo.deviceId, alarmInfo.accountId,
                                            Optional.<List<Alarm>>absent(),
                                            Optional.of(smartAlarmRingTime),
                                            Optional.<DateTimeZone>absent()));
                                    nextRingTime = smartAlarmRingTime;

                                } else {
                                    // Too early to compute smart alarm time. There are two cases:
                                    // 1) Next ring time is already set at this moment, but a regular one.
                                    // 2) this is the first ring for the user, already saved.
                                    // No need to update.
                                }

                            }else{
                                // the smart alarm is generated, and the ring time is not passed yet.
                                // the nextRIngTime here is an out-dated regular ring time.
                                // set nextRingTime to current because the current time is a smart alarm.
                                nextRingTime = currentRingTime;
                            }
                        } else {
                            // the next ringtime is generated by this worker, update ringTime.
                            final DateTime nextRingTimeLocal = new DateTime(nextRingTime.actualRingTimeUTC, alarmInfo.timeZone.get());

                            LOGGER.info("Device {} ring time updated to {}", morpheusId, nextRingTimeLocal);
                            mergedAlarmInfoDynamoDB.setInfo(new AlarmInfo(alarmInfo.deviceId, alarmInfo.accountId,
                                    Optional.<List<Alarm>>absent(),
                                    Optional.of(nextRingTime),
                                    Optional.<DateTimeZone>absent()));
                        }
                    }

                    ringTimes.add(nextRingTime);
                }
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

        final RingTime result = shortedRingTime.length > 0 ? shortedRingTime[0] : RingTime.createEmpty();
        ringTimeDAODynamoDB.setNextRingTime(morpheusId, result);  // Just for backing up the history.
        return result;
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
                if(alarmInfo.alarmList.isPresent()){
                    final List<Alarm> alarms = alarmInfo.alarmList.get();
                    final RingTime nextRingTime = Alarm.Utils.getNextRingTime(alarms, currentTime.getMillis(), userTimeZone);

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

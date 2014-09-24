package com.hello.suripu.workers.alarm;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.workers.pillscorer.PillScoreProcessor;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by pangwu on 9/23/14.
 */
public class AlarmRecordProcessor implements IRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreProcessor.class);
    private final AlarmDAODynamoDB alarmDAODynamoDB;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDAO deviceDAO;

    public AlarmRecordProcessor(final AlarmDAODynamoDB alarmDAODynamoDB,
                                final RingTimeDAODynamoDB ringTimeDAODynamoDB,
                                final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB,
                                final TrackerMotionDAO trackerMotionDAO,
                                final DeviceDAO deviceDAO){
        this.alarmDAODynamoDB = alarmDAODynamoDB;
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.timeZoneHistoryDAODynamoDB = timeZoneHistoryDAODynamoDB;
        this.deviceDAO = deviceDAO;
    }

    @Override
    public void initialize(String s) {
        LOGGER.info("AlarmRecordProcessor initialized: " + s);
    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {

        final Set<String> deviceIds = new HashSet<String>();

        for (final Record record : records) {
            try {
                final InputProtos.periodic_data data = InputProtos.periodic_data.parseFrom(record.getData().array());

                // get MAC address of morpheus
                final byte[] mac = Arrays.copyOf(data.getMac().toByteArray(), 6);
                final String morpheusId = new String(Hex.encodeHex(mac));

                deviceIds.add(morpheusId);


            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            }
        }


        for(final String morpheusId:deviceIds) {
            final List<DeviceAccountPair> deviceAccountPairs = deviceDAO.getAccountIdsForDeviceId(morpheusId);
            final RingTime currentRingTime = this.ringTimeDAODynamoDB.getNextRingTime(morpheusId);

            final ArrayList<RingTime> ringTimes = new ArrayList<RingTime>();
            DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");

            for (final DeviceAccountPair pair : deviceAccountPairs) {
                try {
                    // TODO: Warning, since we query dynamoDB based on user input, the user can generate a lot of
                    // requests to break our bank(Assume that Dynamo DB never goes down).
                    // May be we should somehow cache these data to reduce load & cost.
                    // Could be some simple HashMap class variable that is cleared periodically.

                    // Get the timezone for current user.
                    final Optional<TimeZoneHistory> timeZoneHistoryOptional = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(pair.accountId);
                    if (timeZoneHistoryOptional.isPresent()) {
                        userTimeZone = DateTimeZone.forID(timeZoneHistoryOptional.get().timeZoneId);
                    } else {
                        LOGGER.warn("Alarm worker failed to get user timezone for account {}", pair.accountId);
                    }
                } catch (AmazonServiceException awsException) {
                    // I guess this endpoint should never bail out?
                    LOGGER.error("AWS error when retrieving user timezone for account {}", pair.accountId);
                }


                try {
                    // Get alarms templates for a user
                    final List<Alarm> alarms = this.alarmDAODynamoDB.getAlarms(pair.accountId);

                    // Based on current time, the user's alarm template & user's current timezone, compute
                    // the next ringing moment.

                    // Here we set the current time to 1 minutes before, so we can have one minute drift tolerance.
                    final RingTime nextRingTime = Alarm.Utils.getNextRingTimestamp(alarms, DateTime.now().minusMinutes(1).getMillis(), userTimeZone);
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

            if (shortedRingTime.length > 0) {
                final RingTime nextRingTime = shortedRingTime[0];
                if(currentRingTime.equals(nextRingTime)) {
                    if (currentRingTime.isSmart()) {
                        // Skip, don't need to do anything, wait for update.
                    } else {

                        // TODO: Compute the sleep cycles for smart alarms.
                        // Step1. check if the current time is N min before next ring.
                        // Step2. If yes, compute sleep cycles.
                        // Step3. Update ringTime.

                    }
                }else{
                    // update ringTime.
                    this.ringTimeDAODynamoDB.setNextRingTime(morpheusId, nextRingTime);
                }
            }else{
                // Mark the user don't have alarm.
                this.ringTimeDAODynamoDB.setNextRingTime(morpheusId, RingTime.createEmpty());
            }
        }


        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }
}

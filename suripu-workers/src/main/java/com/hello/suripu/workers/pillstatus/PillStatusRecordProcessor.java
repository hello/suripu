package com.hello.suripu.workers.pillstatus;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.tasks.TaskProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.PillClassificationDAO;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.PillClassification;
import com.hello.suripu.core.util.PillStatus;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by pangwu on 6/23/15.
 */
public class PillStatusRecordProcessor extends HelloBaseRecordProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PillStatusRecordProcessor.class);
    private static final int PILL_BATT_LEVEL_FLUCTUATE_RANGE = 20;

    private final PillHeartBeatDAO pillHeartBeatDAO;
    private final PillClassificationDAO pillClassificationDAO;
    private final DeviceDAO deviceDAO;

    private final Meter notRegisteredPillMeter;
    private final Meter batteryRecoverMeter;
    private final Meter dyingPillMeter;
    private final Meter newConvertedDyingPill;

    public PillStatusRecordProcessor(final DeviceDAO deviceDAO,
                                     final PillHeartBeatDAO pillHeartBeatDAO,
                                     final PillClassificationDAO pillClassificationDAO){
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.deviceDAO = deviceDAO;
        this.pillClassificationDAO = pillClassificationDAO;

        this.notRegisteredPillMeter = Metrics.defaultRegistry().newMeter(
                PillStatusRecordProcessor.class,
                "pill-not-registered",
                "pill-not-registered",
                TimeUnit.SECONDS);
        this.batteryRecoverMeter = Metrics.defaultRegistry().newMeter(
                PillStatusRecordProcessor.class,
                "pill-recover",
                "pill-recover",
                TimeUnit.SECONDS
        );
        this.dyingPillMeter = Metrics.defaultRegistry().newMeter(
                PillStatusRecordProcessor.class,
                "dying-pill-count",
                "dying-pill-count",
                TimeUnit.SECONDS
        );
        this.newConvertedDyingPill = Metrics.defaultRegistry().newMeter(
                PillStatusRecordProcessor.class,
                "detected-dying-pill",
                "detected-dying-pill",
                TimeUnit.SECONDS
        );
    }

    @Override
    public void initialize(final String s) {
        LOGGER.info("PillStatusRecordProcessor initialized: {}", s);
    }


    private List<DeviceStatus> removeDuplicate(final List<DeviceStatus> deviceStatuses){
        final List<DeviceStatus> copy = new ArrayList<>();


        DateTime lastHeartBeatTime = null;
        for(final DeviceStatus deviceStatus:deviceStatuses){
            if((lastHeartBeatTime == null) ||
                    (lastHeartBeatTime != null && !deviceStatus.lastSeen.equals(lastHeartBeatTime))){
                copy.add(deviceStatus);
            }

            lastHeartBeatTime = deviceStatus.lastSeen;
        }

        return copy;
    }


    private float getDrop(final List<DeviceStatus> window, final int windowSize, final int smoothCount){
        if(window.size() < windowSize || window.size() < smoothCount){
            return 0f;
        }

        long startSum = 0;
        long endSum = 0;
        for(int i = 0; i < smoothCount; i++){
            startSum += window.get(i).batteryLevel;
            endSum += window.get(window.size() - i - 1).batteryLevel;
        }

        final float startBatteryLevel = startSum / (float)smoothCount;
        final float endBatteryLevel = endSum / (float)smoothCount;

        final float drop = startBatteryLevel - endBatteryLevel;
        return drop;
    }


    private Optional<Float> getMaxDrop(final List<DeviceStatus> restoredWindow, final DateTime windowStartTime, final int windowSize){

        final List<DeviceStatus> restoredWindowMutable = Lists.newArrayList(restoredWindow);
        Collections.sort(restoredWindowMutable, new Comparator<DeviceStatus>() {
            @Override
            public int compare(final DeviceStatus o1, final DeviceStatus o2) {
                return o1.lastSeen.compareTo(o2.lastSeen);
            }
        });

        final List<DeviceStatus> bufferNoDuplication = removeDuplicate(restoredWindowMutable);
        final LinkedList<DeviceStatus> slidingWindow = new LinkedList<>();
        Optional<Float> maxDrop = Optional.absent();

        for(final DeviceStatus deviceStatus:bufferNoDuplication){
            if(deviceStatus.batteryLevel > 100){
                // skip the level interrupt checker
                continue;
            }

            if(deviceStatus.lastSeen.isBefore(windowStartTime)){
                continue;
            }

            slidingWindow.add(deviceStatus);
            if(slidingWindow.size() == windowSize){

                final float drop = getDrop(slidingWindow, windowSize, 5);
                if(maxDrop.isPresent() == false || drop > maxDrop.get()){
                    maxDrop = Optional.of(drop);
                }
                slidingWindow.removeFirst();
            }
        }

        return maxDrop;

    }

    private void doProcess(final List<Long> internalPillIds){
        final RateLimiter rateLimiter = RateLimiter.create(100d);
        for(final Long internalPillId:internalPillIds){
            rateLimiter.acquire();

            final Optional<PillClassification> pillClassificationOptional = this.pillClassificationDAO.getByInternalPillId(internalPillId);

            // Figure out where we ends the classification so we can reconstruct the sliding window.
            final DateTime queryStartTimeFor24hrWindow = pillClassificationOptional.isPresent() ?
                    pillClassificationOptional.get().last24PointWindowStartTime :
                    new DateTime(0, DateTimeZone.UTC);
            final DateTime queryStartTimeFor72hrWindow = pillClassificationOptional.isPresent() ?
                    pillClassificationOptional.get().last72PointWindowStartTime :
                    new DateTime(0, DateTimeZone.UTC);
            final List<DeviceStatus> dataFromLast72Hours = this.pillHeartBeatDAO.getPillStatusBetweenUTC(internalPillId,
                    queryStartTimeFor72hrWindow,
                    DateTime.now());

            if(dataFromLast72Hours.isEmpty()){
                continue;
            }

            // Extract the two features we need:
            // day to day drop
            // and drop on every 3 days.
            final Optional<Float> maxDrop24hr = getMaxDrop(dataFromLast72Hours,
                    queryStartTimeFor24hrWindow,
                    24);
            final Optional<Float> maxDrop72hr = getMaxDrop(dataFromLast72Hours,
                    queryStartTimeFor72hrWindow,
                    72);

            // Plug it into the decision boundary:
            // -0.0064 * d2d + 0.1281 * d3d - 3.4344 = 0

            if(!maxDrop24hr.isPresent() || !maxDrop72hr.isPresent()){
                LOGGER.debug("Not enough data to classify pill {}", internalPillId);
                continue;
            }

            final DeviceStatus lastHeartBeat = dataFromLast72Hours.get(dataFromLast72Hours.size() - 1);
            final long lastHeartBeatTimeMillis = lastHeartBeat.lastSeen.getMillis();
            final PillClassification classification = new PillClassification(0L, internalPillId,
                    "",   // we should think a way to get this.
                    lastHeartBeatTimeMillis,
                    lastHeartBeatTimeMillis,
                    lastHeartBeat.batteryLevel,
                    Math.max(maxDrop24hr.get(), pillClassificationOptional.isPresent() ? pillClassificationOptional.get().max24HoursBatteryDelta : 0f),
                    Math.max(maxDrop72hr.get(), pillClassificationOptional.isPresent() ? pillClassificationOptional.get().max72HoursBatteryDelta : 0f),
                    PillStatus.NORMAL.toInt()
                    );



            if(!pillClassificationOptional.isPresent()){
                this.pillClassificationDAO.insert(classification);
            }else{
                this.pillClassificationDAO.update(classification);
            }
            this.newConvertedDyingPill.mark();

        }
    }

    @Override
    public void processRecords(final List<Record> list, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        for(final Record record:list){
            try {
                final TaskProtos.WorkerTask workerTask = TaskProtos.WorkerTask.parseFrom(record.getData().array());
                if(workerTask.getTaskType() != TaskProtos.WorkerTask.TaskType.PILL_CLASSIFICATION){
                    continue;
                }

                final List<Long> internalPillIdsToBeClassified = new ArrayList<>();
                final List<String> pillIds = workerTask.getPillIdsList();

                for(final String pillId:pillIds){
                    final Optional<DeviceAccountPair> registeredPill = this.deviceDAO.getInternalPillId(pillId);
                    if(!registeredPill.isPresent()){
                        LOGGER.info("Pill {} not found in tracker account map, pill might be unpaired.");
                        continue;
                    }

                    internalPillIdsToBeClassified.add(registeredPill.get().internalDeviceId);
                }


                if(workerTask.getPillIdsCount() == 0){
                    final List<Long> pillsSeenFromLast24HoursAndHaveLessThan80PercentBattery = this.pillHeartBeatDAO.getPillIdsSeenInLast24Hours();
                    LOGGER.info("Found {} pills to be classified.", pillsSeenFromLast24HoursAndHaveLessThan80PercentBattery.size());

                    internalPillIdsToBeClassified.addAll(pillsSeenFromLast24HoursAndHaveLessThan80PercentBattery);
                }

                doProcess(internalPillIdsToBeClassified);

            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
            }
        }



        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
        System.exit(1);
    }
}

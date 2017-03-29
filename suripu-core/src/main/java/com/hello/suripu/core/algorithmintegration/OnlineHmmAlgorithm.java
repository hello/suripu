package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.TimelineError;
import com.hello.suripu.core.util.TimelineSafeguards;
import com.hello.suripu.core.util.TimelineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by benjo on 1/20/16.
 */
public class OnlineHmmAlgorithm implements TimelineAlgorithm {

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(OnlineHmmAlgorithm.class);

    final TimelineUtils timelineUtils;
    final TimelineSafeguards timelineSafeguards;
    final Optional<UUID> uuid;
    final Logger LOGGER;
    final OnlineHmmModelsDAO priorsDAO;
    final DefaultModelEnsembleDAO defaultModelEnsembleDAO;
    final FeatureExtractionModelsDAO featureExtractionModelsDAO;

    public OnlineHmmAlgorithm(final OnlineHmmModelsDAO priorsDAO,final DefaultModelEnsembleDAO defaultModelEnsembleDAO,final FeatureExtractionModelsDAO featureExtractionModelsDAO,final Optional<UUID> uuid) {
        timelineUtils = new TimelineUtils(uuid);
        timelineSafeguards = new TimelineSafeguards(uuid);
        LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid);
        this.uuid = uuid;
        this.priorsDAO = priorsDAO;
        this.defaultModelEnsembleDAO = defaultModelEnsembleDAO;
        this.featureExtractionModelsDAO = featureExtractionModelsDAO;
    }



    @Override
    public Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData sensorData, final SleepPeriod sleepPeriod, final TimelineLog log, final long accountId, final boolean feedbackChanged, final Set<String> features) {

        LOGGER.info("alg=ONLINE-HMM account_id={}",accountId);

        try {

            final OnlineHmm onlineHmm = new OnlineHmm(defaultModelEnsembleDAO, featureExtractionModelsDAO, priorsDAO, uuid);


            final SleepEvents<Optional<Event>> events = onlineHmm.predictAndUpdateWithLabels(
                    accountId,
                    sensorData.date,
                    sensorData.startTimeLocalUTC,
                    sensorData.endTimeLocalUTC,
                    sensorData.currentTimeUTC.plusMillis(sensorData.timezoneOffsetMillis),
                    sensorData,
                    feedbackChanged,
                    false);

            final boolean isPrimarySleepPeriod = true;
            //verify that algorithm produced something useable
            final TimelineError error = timelineSafeguards.checkIfValidTimeline(
                    accountId,
                    true,
                    Optional.absent(),
                    AlgorithmType.ONLINE_HMM,
                    events,
                    ImmutableList.copyOf(Collections.EMPTY_LIST),
                    ImmutableList.copyOf(sensorData.allSensorSampleList.get(Sensor.LIGHT)),
                    ImmutableList.copyOf(sensorData.oneDaysTrackerMotion.processedtrackerMotions));

            LOGGER.info("alg_status={} account_id={} date={}",error,accountId,sensorData.date.toDate());

            if (!error.equals(TimelineError.NO_ERROR)) {
                log.addMessage(AlgorithmType.ONLINE_HMM, error);
                return Optional.absent();
            }

            final List<Event> eventsList = timelineUtils.eventsFromOptionalEvents(events.toList());
            log.addMessage(AlgorithmType.ONLINE_HMM, eventsList);

            return Optional.of(new TimelineAlgorithmResult(AlgorithmType.ONLINE_HMM,eventsList));
        }
        catch (Exception e) {
            log.addMessage(AlgorithmType.ONLINE_HMM, TimelineError.UNEXEPECTED);
            LOGGER.error("alg_status={} account_id={} date={}","exception",accountId,sensorData.date.toDate());
        }

        return Optional.absent();
    }

    @Override
    public TimelineAlgorithm cloneWithNewUUID(final Optional<UUID> uuid) {
        return new OnlineHmmAlgorithm(priorsDAO,defaultModelEnsembleDAO,featureExtractionModelsDAO,uuid);
    }
}

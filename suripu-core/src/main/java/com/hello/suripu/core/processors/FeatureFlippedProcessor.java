package com.hello.suripu.core.processors;

import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.librato.rollout.RolloutClient;

import javax.inject.Inject;
import java.util.Collections;

/**
 * Created by pangwu on 3/17/15.
 */
public class FeatureFlippedProcessor {
    @Inject
    protected RolloutClient featureFlipper;

    protected FeatureFlippedProcessor()  {
        ObjectGraphRoot.getInstance().inject(this);
    }

    protected Integer missingDataDefaultValue(final Long accountId) {
        boolean active = featureFlipper.userFeatureActive(FeatureFlipper.MISSING_DATA_DEFAULT_VALUE, accountId, Collections.EMPTY_LIST);
        return (active) ? -1 : 0;
    }

    protected Boolean hasAlarmInTimeline(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.ALARM_IN_TIMELINE, accountId, Collections.EMPTY_LIST);
    }
    protected Boolean hasSoundInTimeline(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SOUND_EVENTS_IN_TIMELINE, accountId, Collections.EMPTY_LIST);
    }
    protected Boolean hasEnvironmentInTimelineScore(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.ENVIRONMENT_IN_TIMELINE_SCORE, accountId, Collections.EMPTY_LIST);
    }
    protected Boolean hasFeedbackInTimeline(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.FEEDBACK_IN_TIMELINE, accountId, Collections.EMPTY_LIST);
    }
    protected Boolean hasInOrOutOfBedEvents(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.IN_OUT_BED_EVENTS, accountId, Collections.EMPTY_LIST);
    }
    protected Boolean hasHmmEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.HMM_ALGORITHM, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasExtraEventsEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.EXTRA_EVENTS, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasVotingEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.VOTING_ALGORITHM, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasOnlineHmmAlgorithmEnabled(final Long accountId) {
        return  featureFlipper.userFeatureActive(FeatureFlipper.ONLINE_HMM_ALGORITHM,accountId,Collections.EMPTY_LIST);
    }

    protected Boolean hasOnlineHmmLearningEnabled(final Long accountId) {
        return  featureFlipper.userFeatureActive(FeatureFlipper.ONLINE_HMM_LEARNING,accountId,Collections.EMPTY_LIST);
    }

    protected Boolean hasPartnerFilterEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.PARTNER_FILTER, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasHmmPartnerFilterEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.HMM_PARTNER_FILTER, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasAllSensorQueryUseUTCTs(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.ALL_SENSOR_QUERY_USE_UTC_TS, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasRemoveMotionEventsOutsideSleep(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.REMOVE_MOTION_EVENTS_OUTSIDE_SLEEP, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasRemoveGreyOutEvents(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.REMOVE_GREY_OUT_EVENTS, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasSleepScoreDurationWeighting(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_DURATION_WEIGHTING, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasCalibrationEnabled(final String senseId) {
        return featureFlipper.deviceFeatureActive(FeatureFlipper.CALIBRATION, senseId, Collections.EMPTY_LIST);
    }

    protected Boolean hasTimelineOrderEnforcement(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.TIMELINE_EVENT_ORDER_ENFORCEMENT, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasInvalidSleepScoreFromFeedbackChecking(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.TIMELINE_EVENT_SLEEP_SCORE_ENFORCEMENT, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasDustSmoothEnabled(final String senseId) {
        return featureFlipper.deviceFeatureActive(FeatureFlipper.DUST_SMOOTH, senseId, Collections.EMPTY_LIST);
    }

    protected boolean hasTimelineInSleepInsights(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.TIMELINE_IN_SLEEP_INSIGHTS, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean isSenseLastSeenDynamoDBReadEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SENSE_LAST_SEEN_VIEW_DYNAMODB_READ, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean isSensorsDBUnavailable(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SENSORS_DB_UNAVAILABLE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasDeviceDataDynamoDBTimelineEnabled(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.DYNAMODB_DEVICE_DATA_TIMELINE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasSleepSegmentOffsetRemapping(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SEGMENT_OFFSET_REMAPPING, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasPillDataDynamoDBTimelineEnabled(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.DYNAMODB_PILL_DATA_TIMELINE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasOutlierFilterEnabled(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.OUTLIER_FILTER,accountId,Collections.EMPTY_LIST);
    }

    protected Boolean hasAnomalyLightQuestionEnabled(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.QUESTION_ANOMALY_LIGHT_VISIBLE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasSleepScoreDurationV2(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_DURATION_V2, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasSleepStatMediumSleep(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_STATS_MEDIUM_SLEEP, accountId, Collections.EMPTY_LIST);
    }

}

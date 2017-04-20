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

    protected Boolean hasNeuralNetAlgorithmEnabled(final Long accountId) {
        return  featureFlipper.userFeatureActive(FeatureFlipper.NEURAL_NET_ALGORITHM,accountId,Collections.EMPTY_LIST);
    }

    protected Boolean hasNeuralNetFourEventsAlgorithmEnabled(final Long accountId) {
        return  featureFlipper.userFeatureActive(FeatureFlipper.NEURAL_NET_FOUR_EVENTS_ALGORITHM,accountId,Collections.EMPTY_LIST);
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

    protected Boolean hasSleepScoreDurationWeighting(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_DURATION_WEIGHTING, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasCalibrationEnabled(final String senseId) {
        return featureFlipper.deviceFeatureActive(FeatureFlipper.CALIBRATION, senseId, Collections.EMPTY_LIST);
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

    protected Boolean hasSleepScoreDurationWeightingV2(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_DURATION_WEIGHTING_V2, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasSleepStatMediumSleep(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_STATS_MEDIUM_SLEEP, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasTimesAwakeSleepScorePenalty(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_TIMES_AWAKE_PENALTY, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useAudioPeakEnergy(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.AUDIO_PEAK_ENERGY_DB, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasInBedSearchEnabled(final long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.IN_BED_SEARCH,accountId,Collections.EMPTY_LIST);
    }

    protected Boolean hasSleepSoundsEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SOUNDS_ENABLED, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasCBTIGoalGoOutside(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.CBTI_GOAL_GO_OUTSIDE, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasRemovePairingMotions(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.PILL_PAIR_MOTION_FILTER, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasOffBedFilterEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.OFF_BED_HMM_MOTION_FILTER, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useHigherThesholdForSoundEvents(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SOUND_EVENTS_USE_HIGHER_THRESHOLD, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useSleepScoreV3(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_V3, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useSleepScoreV4(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_V4, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useQuestionAskTime(final Long accountId){
        return featureFlipper.userFeatureActive(FeatureFlipper.QUESTION_ASK_TIME_ENABLED, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useNoMotionEnforcement(final Long accountId){
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_NO_MOTION_ENFORCEMENT, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useSleepScoreV5(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_SCORE_V5, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useUninterruptedDuration(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.SLEEP_STATS_UNINTERRUPTED_SLEEP, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useSmartAlarmRefactored(final Long accountId){
        return featureFlipper.userFeatureActive(FeatureFlipper.SMART_ALARM_REFACTORED, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useTimelineSleepPeriods(final Long accountId){
        return featureFlipper.userFeatureActive(FeatureFlipper.TIMELINE_SLEEP_PERIOD, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean isDaySleeper(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.DAY_SLEEPER, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasMotionMaskPartnerFilter(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.MOTION_MASK_PARTNER_FILTER, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useTimelineLockdown(final Long accountId){
        return featureFlipper.userFeatureActive(FeatureFlipper.TIMELINE_LOCKDOWN, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean useHigherMotionAmplitudeThreshold(final Long accountId){
        return featureFlipper.userFeatureActive(FeatureFlipper.MIN_MOTION_AMPLITUDE_HIGH_THRESHOLD, accountId, Collections.EMPTY_LIST);
    }

}

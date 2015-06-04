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

    protected Boolean hasPartnerFilterEnabled(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.PARTNER_FILTER, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasAllSensorQueryUseUTCTs(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.ALL_SENSOR_QUERY_USE_UTC_TS, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasPillCountFilter(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.PILL_COUNT_FILTER, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasRemoveMotionEventsOutsideSleep(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.REMOVE_MOTION_EVENTS_OUTSIDE_SLEEP, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasRemoveGreyOutEvents(final Long accountId) {
        return featureFlipper.userFeatureActive(FeatureFlipper.REMOVE_GREY_OUT_EVENTS, accountId, Collections.EMPTY_LIST);
    }

}

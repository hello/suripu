package com.hello.suripu.core.flipper;

import com.librato.rollout.RolloutClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pangwu on 3/16/15.
 */
public class FlipperParams {
    private Map<String, Boolean> paramsMap = new HashMap<>();
    public void setFeature(final String featureString, final Boolean value){
        this.paramsMap.put(featureString, value);
    }

    public boolean hasFeature(final String featureString){
        if(!paramsMap.containsKey(featureString)){
            return false;
        }
        return this.paramsMap.get(featureString);
    }

    private FlipperParams(){

    }

    public static FlipperParams create(final Long accountId, final RolloutClient featureFlipper){
        final FlipperParams flipperParams = new FlipperParams();
        flipperParams.setFeature(FeatureFlipper.ALARM_IN_TIMELINE,
                featureFlipper.userFeatureActive(FeatureFlipper.ALARM_IN_TIMELINE, accountId, Collections.EMPTY_LIST));
        flipperParams.setFeature(FeatureFlipper.SMART_ALARM,
                featureFlipper.userFeatureActive(FeatureFlipper.SMART_ALARM, accountId, Collections.EMPTY_LIST));
        flipperParams.setFeature(FeatureFlipper.SOUND_INFO_TIMELINE,
                featureFlipper.userFeatureActive(FeatureFlipper.SOUND_INFO_TIMELINE, accountId, Collections.EMPTY_LIST));
        flipperParams.setFeature(FeatureFlipper.SOUND_EVENTS_IN_TIMELINE,
                featureFlipper.userFeatureActive(FeatureFlipper.SOUND_EVENTS_IN_TIMELINE, accountId, Collections.EMPTY_LIST));
        flipperParams.setFeature(FeatureFlipper.FEEDBACK_IN_TIMELINE,
                featureFlipper.userFeatureActive(FeatureFlipper.FEEDBACK_IN_TIMELINE, accountId, Collections.EMPTY_LIST));
        flipperParams.setFeature(FeatureFlipper.IN_OUT_BED_EVENTS,
                featureFlipper.userFeatureActive(FeatureFlipper.IN_OUT_BED_EVENTS, accountId, Collections.EMPTY_LIST));
        flipperParams.setFeature(FeatureFlipper.HMM_ALGORITHM,
                featureFlipper.userFeatureActive(FeatureFlipper.HMM_ALGORITHM, accountId, Collections.EMPTY_LIST));
        flipperParams.setFeature(FeatureFlipper.PARTNER_FILTER,
                featureFlipper.userFeatureActive(FeatureFlipper.PARTNER_FILTER, accountId, Collections.EMPTY_LIST));
        flipperParams.setFeature(FeatureFlipper.PUSH_NOTIFICATIONS_ENABLED,
                featureFlipper.userFeatureActive(FeatureFlipper.PUSH_NOTIFICATIONS_ENABLED, accountId, Collections.EMPTY_LIST));
        return flipperParams;
    }
}

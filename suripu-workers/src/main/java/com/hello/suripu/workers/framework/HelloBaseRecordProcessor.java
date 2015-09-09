package com.hello.suripu.workers.framework;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.librato.rollout.RolloutClient;

import javax.inject.Inject;
import java.util.Collections;

/**
 * Created by pangwu on 12/4/14.
 */
public abstract class HelloBaseRecordProcessor implements IRecordProcessor {
    @Inject
    protected RolloutClient flipper;

    public HelloBaseRecordProcessor(){
        ObjectGraphRoot.getInstance().inject(this);
    }


    protected Integer missingDataDefaultValue(final Long accountId) {
        boolean active = flipper.userFeatureActive(FeatureFlipper.MISSING_DATA_DEFAULT_VALUE, accountId, Collections.EMPTY_LIST);
        return (active) ? -1 : 0;
    }

    protected Boolean userHasPushNotificationsEnabled(final Long accountId) {
        return flipper.userFeatureActive(FeatureFlipper.PUSH_NOTIFICATIONS_ENABLED, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasHmmEnabled(final Long accountId) {
        return  flipper.userFeatureActive(FeatureFlipper.HMM_ALGORITHM,accountId,Collections.EMPTY_LIST);
    }

    protected Boolean hasSmartAlarmLogEnabled(final Long accountId){
        return flipper.userFeatureActive(FeatureFlipper.SMART_ALARM_LOGGING, accountId, Collections.EMPTY_LIST);
    }

    protected Boolean hasLastSeenViewDynamoDBEnabled(final String senseId){
        return flipper.deviceFeatureActive(FeatureFlipper.SENSE_LAST_SEEN_VIEW_DYNAMODB, senseId, Collections.EMPTY_LIST);
    }

    protected Boolean hasPillLastSeenDynamoDBEnabled(final String senseId) {
        return flipper.deviceFeatureActive(FeatureFlipper.PILL_LAST_SEEN_DYNAMODB,senseId, Collections.EMPTY_LIST);
    }

    protected Boolean hasKinesisTimezonesEnabled(final String senseId) {
        return flipper.deviceFeatureActive(FeatureFlipper.WORKER_KINESIS_TIMEZONES, senseId, Collections.EMPTY_LIST);
    }

    protected Boolean hasCalibrationEnabled(final String senseId) {
        return flipper.deviceFeatureActive(FeatureFlipper.CALIBRATION, senseId, Collections.EMPTY_LIST);
    }

}

package com.hello.suripu.workers.framework;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.aphyr.riemann.client.RiemannClient;
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

    @Inject
    protected RiemannClient riemannClient;

    public HelloBaseRecordProcessor(){
        ObjectGraphRoot.getInstance().inject(this);
    }

    protected Boolean userHasPushNotificationsEnabled(final Long accountId) {
        return flipper.userFeatureActive(FeatureFlipper.PUSH_NOTIFICATIONS_ENABLED, accountId, Collections.EMPTY_LIST);
    }
}

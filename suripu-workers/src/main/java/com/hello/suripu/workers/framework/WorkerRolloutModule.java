package com.hello.suripu.workers.framework;

import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.flipper.DynamoDBAdapter;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.workers.alarm.AlarmRecordProcessor;
import com.hello.suripu.workers.insights.InsightsGenerator;
import com.hello.suripu.workers.notifications.PushNotificationsProcessor;
import com.hello.suripu.workers.pill.S3RecordProcessor;
import com.hello.suripu.workers.pill.SavePillDataProcessor;
import com.hello.suripu.workers.pillscorer.PillScoreProcessor;
import com.hello.suripu.workers.sense.SenseSaveProcessor;
import com.hello.suripu.workers.timeline.TimelineRecordProcessor;
import com.librato.rollout.RolloutAdapter;
import com.librato.rollout.RolloutClient;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Created by pangwu on 12/4/14.
 */
@Module(injects = {
        AlarmRecordProcessor.class,
        S3RecordProcessor.class,
        SavePillDataProcessor.class,
        PillScoreProcessor.class,
        SenseSaveProcessor.class,
        InsightsGenerator.class,
        PushNotificationsProcessor.class,
        TimelineRecordProcessor.class,
        TimelineProcessor.class,
})
public class WorkerRolloutModule {
    private final FeatureStore featureStore;
    private final Integer pollingIntervalInSeconds;

    public WorkerRolloutModule(final FeatureStore featureStore, final Integer pollingIntervalInSeconds) {
        this.featureStore = featureStore;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
    }

    @Provides
    @Singleton
    RolloutAdapter providesRolloutAdapter() {
        return new DynamoDBAdapter(featureStore, pollingIntervalInSeconds);
    }

    @Provides
    @Singleton
    RolloutClient providesRolloutClient(RolloutAdapter adapter) {
        return new RolloutClient(adapter);
    }
}

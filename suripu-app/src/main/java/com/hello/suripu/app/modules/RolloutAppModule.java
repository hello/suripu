package com.hello.suripu.app.modules;

import com.hello.suripu.app.resources.v1.DataScienceResource;
import com.hello.suripu.app.resources.v1.RoomConditionsResource;
import com.hello.suripu.app.resources.v1.TimelineResource;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.flipper.DynamoDBAdapter;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.librato.rollout.RolloutAdapter;
import com.librato.rollout.RolloutClient;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(injects = {
        TimelineResource.class,
        RoomConditionsResource.class,
        DataScienceResource.class,
        TimelineProcessor.class,
})
public class RolloutAppModule {
    private final FeatureStore featureStore;
    private final Integer pollingIntervalInSeconds;

    public RolloutAppModule(final FeatureStore featureStore, final Integer pollingIntervalInSeconds) {
        this.featureStore = featureStore;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
    }

    @Provides @Singleton
    RolloutAdapter providesRolloutAdapter() {
        return new DynamoDBAdapter(featureStore, pollingIntervalInSeconds);
    }

    @Provides @Singleton
    RolloutClient providesRolloutClient(RolloutAdapter adapter) {
        return new RolloutClient(adapter);
    }
}

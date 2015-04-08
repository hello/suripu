package com.hello.suripu.service.modules;

import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.flipper.DynamoDBAdapter;
import com.hello.suripu.service.resources.AudioResource;
import com.hello.suripu.service.resources.CheckResource;
import com.hello.suripu.service.resources.ProvisionResource;
import com.hello.suripu.service.resources.ReceiveResource;
import com.hello.suripu.service.resources.RegisterResource;
import com.librato.rollout.RolloutAdapter;
import com.librato.rollout.RolloutClient;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(injects = {
        ReceiveResource.class,
        AudioResource.class,
        RegisterResource.class,
        CheckResource.class,
        ProvisionResource.class
})
public class RolloutModule {
    private final FeatureStore featureStore;
    private final Integer pollingIntervalInSeconds;

    public RolloutModule(final FeatureStore featureStore, final Integer pollingIntervalInSeconds) {
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

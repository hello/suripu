package com.hello.suripu.service.modules;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.hello.suripu.core.flipper.DynamoDBAdapter;
import com.hello.suripu.service.resources.ReceiveResource;
import com.librato.rollout.RolloutAdapter;
import com.librato.rollout.RolloutClient;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(injects = {
        ReceiveResource.class
})
public class RolloutModule {
    private final AmazonDynamoDBClient dynamoDB;
    private final Integer pollingIntervalInSeconds;
    private final String namespace;

    public RolloutModule(final AmazonDynamoDBClient dynamoDB, final Integer pollingIntervalInSeconds, final String namespace) {
        this.dynamoDB = dynamoDB;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
        this.namespace = namespace;
    }

    @Provides @Singleton
    RolloutAdapter providesRolloutAdapter() {
        return new DynamoDBAdapter(dynamoDB, pollingIntervalInSeconds, namespace);
    }

    @Provides @Singleton
    RolloutClient providesRolloutClient(RolloutAdapter adapter) {
        return new RolloutClient(adapter);
    }
}

package com.hello.suripu.workers.framework;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.aphyr.riemann.client.RiemannClient;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.flipper.DynamoDBAdapter;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.workers.alarm.AlarmRecordProcessor;
import com.hello.suripu.workers.insights.InsightsGenerator;
import com.hello.suripu.workers.logs.LogIndexerProcessor;
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
import java.io.IOException;

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
        LogIndexerProcessor.class,
})
public class WorkerRolloutModule {
    private final FeatureStore featureStore;
    private final Integer pollingIntervalInSeconds;
    private final String riemannHost;
    private final static Integer DEFAULT_POLLING_INTERVAL_SECONDS = 30;
    private final static String DEFAULT_RIEMANN_HOST = "riemann.internal.hello.is";

    private WorkerRolloutModule(final FeatureStore featureStore, final Integer pollingIntervalInSeconds, final String riemannHost) {
        this.featureStore = featureStore;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
        this.riemannHost = riemannHost;
    }

    @Deprecated
    public WorkerRolloutModule(final FeatureStore featureStore, final Integer pollingIntervalInSeconds) {
        this.featureStore = featureStore;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
        this.riemannHost = DEFAULT_RIEMANN_HOST;
    }

    public static WorkerRolloutModule create(final AWSCredentialsProvider awsCredentialsProvider, final WorkerConfiguration workerConfiguration) {
        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(workerConfiguration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String featureNamespace = (workerConfiguration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);

        return new WorkerRolloutModule(featureStore, DEFAULT_POLLING_INTERVAL_SECONDS, workerConfiguration.getRiemannConfiguration().getHost());
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


    @Provides
    @Singleton
    RiemannClient providesRiemannClient() {
        try {
            return RiemannClient.tcp(riemannHost, 5555);
        } catch (IOException e) {
            throw new RuntimeException("Failed creating Riemann client: " + e.getMessage());
        }
    }
}

package com.hello.suripu.workers.notifications;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.coredw.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import com.hello.suripu.core.notifications.NotificationSubscriptionsReadDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import org.skife.jdbi.v2.DBI;

public class PushNotificationsProcessorFactory implements IRecordProcessorFactory {

    private final PushNotificationsWorkerConfiguration configuration;
    private final AWSCredentialsProvider awsCredentialsProvider;

    public PushNotificationsProcessorFactory(final PushNotificationsWorkerConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        this.configuration = configuration;
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    @Override
    public IRecordProcessor createProcessor()  {

        try {

            final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
            final ManagedDataSource commonDataSource = managedDataSourceFactory.build(configuration.getCommonDB());

            final DBI commonSensor = new DBI(commonDataSource);
            commonSensor.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
            commonSensor.registerContainerFactory(new ImmutableListContainerFactory());
            commonSensor.registerContainerFactory(new ImmutableSetContainerFactory());
            commonSensor.registerContainerFactory(new OptionalContainerFactory());
            commonSensor.registerArgumentFactory(new JodaArgumentFactory());

            final NotificationSubscriptionsReadDAO notificationSubscriptionsDAO = commonSensor.onDemand(NotificationSubscriptionsReadDAO.class);
            final AmazonSNS amazonSNS = new AmazonSNSClient(awsCredentialsProvider);
            final MobilePushNotificationProcessor pushNotificationProcessor = new MobilePushNotificationProcessor(amazonSNS, notificationSubscriptionsDAO);

            final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
            final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
            final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
            final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);

            final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
            ObjectGraphRoot.getInstance().init(workerRolloutModule);

            final AmazonDynamoDB mergedUserInfoDynamoDBClient = amazonDynamoDBClientFactory.getForEndpoint(configuration.getAlarmInfoDynamoDBConfiguration().getEndpoint());
            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(mergedUserInfoDynamoDBClient, configuration.getAlarmInfoDynamoDBConfiguration().getTableName());

            final AmazonDynamoDB accountPreferencesDynamoDBClient = amazonDynamoDBClientFactory.getForEndpoint(configuration.getAccountPreferences().getEndpoint());
            final AccountPreferencesDynamoDB accountPreferencesDynamoDB = AccountPreferencesDynamoDB.create(accountPreferencesDynamoDBClient, configuration.getAccountPreferences().getTableName());

            return new PushNotificationsProcessor(pushNotificationProcessor, mergedUserInfoDynamoDB, accountPreferencesDynamoDB, configuration.getActiveHours());

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}

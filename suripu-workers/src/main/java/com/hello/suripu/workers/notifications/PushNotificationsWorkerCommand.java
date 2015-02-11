package com.hello.suripu.workers.notifications;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import com.hello.suripu.core.notifications.NotificationSubscriptionsDAO;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class PushNotificationsWorkerCommand extends ConfiguredCommand<PushNotificationWorkerConfiguration>{

    private final static Logger LOGGER = LoggerFactory.getLogger(PushNotificationsWorkerCommand.class);

    public PushNotificationsWorkerCommand(final String name, final String description) {
        super(name, description);
    }

    @Override
    protected void run(Bootstrap<PushNotificationWorkerConfiguration> bootstrap, Namespace namespace, PushNotificationWorkerConfiguration configuration) throws Exception {

        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource commonDataSource = managedDataSourceFactory.build(configuration.getCommonDB());

        final DBI commonSensor = new DBI(commonDataSource);
        commonSensor.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
        commonSensor.registerContainerFactory(new ImmutableListContainerFactory());
        commonSensor.registerContainerFactory(new ImmutableSetContainerFactory());
        commonSensor.registerContainerFactory(new OptionalContainerFactory());
        commonSensor.registerArgumentFactory(new JodaArgumentFactory());

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final NotificationSubscriptionsDAO notificationSubscriptionsDAO = commonSensor.onDemand(NotificationSubscriptionsDAO.class);
        final AmazonSNS amazonSNS = new AmazonSNSClient(awsCredentialsProvider);
        final MobilePushNotificationProcessor pushNotificationProcessor = new MobilePushNotificationProcessor(amazonSNS, notificationSubscriptionsDAO);

        final ImmutableMap<QueueName, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueName.SENSE_SENSORS_DATA);
        LOGGER.info("\n\n\n!!! This worker is using the following queue: {} !!!\n\n\n", queueName);


        final String workerId = InetAddress.getLocalHost().getCanonicalHostName();
        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppName(),
                queueName,
                awsCredentialsProvider,
                workerId);
        kinesisConfig.withMaxRecords(configuration.getMaxRecords());
        kinesisConfig.withKinesisEndpoint(configuration.getKinesisEndpoint());
        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);

        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        final AmazonDynamoDB mergedUserInfoDynamoDBClient = amazonDynamoDBClientFactory.getForEndpoint(configuration.getAlarmInfoDynamoDBConfiguration().getEndpoint());
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(mergedUserInfoDynamoDBClient, configuration.getAlarmInfoDynamoDBConfiguration().getTableName());
        final IRecordProcessorFactory factory = new PushNotificationsProcessorFactory(pushNotificationProcessor, mergedUserInfoDynamoDB);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();


    }
}

package com.hello.suripu.workers.timeline;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.AlgorithmTestDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SleepHmmDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.workers.framework.WorkerEnvironmentCommand;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Created by pangwu on 9/23/14.
 */
public class TimelineWorkerCommand extends WorkerEnvironmentCommand<TimelineWorkerConfiguration> {
    private final static Logger LOGGER = LoggerFactory.getLogger(TimelineWorkerCommand.class);

    public TimelineWorkerCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, TimelineWorkerConfiguration configuration) throws Exception {


        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();

        final DBI commonDB = new DBI(managedDataSourceFactory.build(configuration.getCommonDB()));
        final DBI sensorsDB = new DBI(managedDataSourceFactory.build(configuration.getSensorsDB()));

        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        sensorsDB.registerContainerFactory(new ImmutableListContainerFactory());
        sensorsDB.registerContainerFactory(new ImmutableSetContainerFactory());


        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());

        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);

        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);

        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.withConnectionTimeout(200); // in ms
        clientConfiguration.withMaxErrorRetry(1);
        final AmazonS3 amazonS3 = new AmazonS3Client(awsCredentialsProvider, clientConfiguration);

        final AmazonDynamoDB mergedUserInfoDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getAlarmDBConfiguration().getEndpoint());
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(mergedUserInfoDynamoDBClient,
                configuration.getUserInfoDynamoDBConfiguration().getTableName());


        final AmazonDynamoDB ringTimeDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getRingTimeHistoryDBConfiguration().getEndpoint());
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(ringTimeDynamoDBClient, configuration.getRingTimeHistoryDBConfiguration().getTableName());

        final AmazonDynamoDB featureDynamoDB = dynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);

        final AmazonDynamoDB sleepHmmDynamoDbClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepHmmDBConfiguration().getEndpoint());
        final String sleepHmmTableName = configuration.getSleepHmmDBConfiguration().getTableName();
        final SleepHmmDAODynamoDB sleepHmmDAODynamoDB = new SleepHmmDAODynamoDB(sleepHmmDynamoDbClient,sleepHmmTableName);

        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepStatsDBConfiguration().getEndpoint());
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(
                dynamoDBStatsClient,
                configuration.getSleepStatsDBConfiguration().getTableName(),
                configuration.getSleepStatsVersion()
        );


        final AmazonDynamoDB dynamoDBTimelineClient = dynamoDBClientFactory.getForEndpoint(configuration.getTimelineDBConfiguration().getEndpoint());
        final TimelineDAODynamoDB timelineDAODynamoDB = new TimelineDAODynamoDB(
                dynamoDBTimelineClient,
                configuration.getTimelineDBConfiguration().getTableName(),
                configuration.getMaxCacheRefreshDay());

        final AmazonDynamoDB algorithmTestDynamoDbClient = dynamoDBClientFactory.getForEndpoint(configuration.getAlgorithmTestDBConfiguration().getEndpoint());
        final AlgorithmTestDAODynamoDB algorithmTestDAODynamoDB = new AlgorithmTestDAODynamoDB(algorithmTestDynamoDbClient,
                configuration.getAlgorithmTestDBConfiguration().getTableName());

        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        final TimelineProcessor timelineProcessor = new TimelineProcessor(trackerMotionDAO,
                deviceDAO, deviceDataDAO,
                ringTimeHistoryDAODynamoDB,
                feedbackDAO,
                sleepHmmDAODynamoDB,
                accountDAO,
                sleepStatsDAODynamoDB);

        final ImmutableMap<QueueName, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueName.BATCH_PILL_DATA);
        LOGGER.info("\n\n\n!!! This worker is using the following queue: {} !!!\n\n\n", queueName);


        final String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppName(),
                queueName,
                awsCredentialsProvider,
                workerId);
        kinesisConfig.withMaxRecords(configuration.getMaxRecords());
        kinesisConfig.withKinesisEndpoint(configuration.getKinesisEndpoint());
        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.LATEST);


        final IRecordProcessorFactory factory = new TimelineRecordProcessorFactory(timelineProcessor,
                deviceDAO,
                mergedUserInfoDynamoDB,
                timelineDAODynamoDB,
                algorithmTestDAODynamoDB,
                configuration);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }
}

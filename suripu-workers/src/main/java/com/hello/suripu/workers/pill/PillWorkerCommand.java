package com.hello.suripu.workers.pill;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
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

public final class PillWorkerCommand extends ConfiguredCommand<PillWorkerConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillWorkerCommand.class);

    public PillWorkerCommand(String name, String description) {
        super(name, description);
    }

    @Override
    public final void run(Bootstrap<PillWorkerConfiguration> bootstrap, Namespace namespace, PillWorkerConfiguration configuration) throws Exception {
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource sensorDataSource = managedDataSourceFactory.build(configuration.getSensorDB());

        final DBI jdbiSensor = new DBI(sensorDataSource);
        jdbiSensor.registerArgumentFactory(new OptionalArgumentFactory(configuration.getSensorDB().getDriverClass()));
        jdbiSensor.registerContainerFactory(new ImmutableListContainerFactory());
        jdbiSensor.registerContainerFactory(new ImmutableSetContainerFactory());
        jdbiSensor.registerContainerFactory(new OptionalContainerFactory());
        jdbiSensor.registerArgumentFactory(new JodaArgumentFactory());



        final ManagedDataSource commonDataSource = managedDataSourceFactory.build(configuration.getSensorDB());

        final DBI jdbiCommon = new DBI(commonDataSource);
        jdbiCommon.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
        jdbiCommon.registerContainerFactory(new ImmutableListContainerFactory());
        jdbiCommon.registerContainerFactory(new ImmutableSetContainerFactory());
        jdbiCommon.registerContainerFactory(new OptionalContainerFactory());
        jdbiCommon.registerArgumentFactory(new JodaArgumentFactory());

        final TrackerMotionDAO trackerMotionDAO = jdbiSensor.onDemand(TrackerMotionDAO.class);
        final DeviceDAO deviceDAO = jdbiCommon.onDemand(DeviceDAO.class);

        final ImmutableMap<QueueName, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueName.BATCH_PILL_DATA);
        LOGGER.info("\n\n\n!!! This worker is using the following queue: {} !!!\n\n\n", queueName);

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
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

        final AmazonDynamoDB mergedUserInfoDynamoDBClient = amazonDynamoDBClientFactory.getForEndpoint(configuration.getUserInfo().getEndpoint());
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(mergedUserInfoDynamoDBClient, configuration.getUserInfo().getTableName());

        final PillHeartBeatDAO heartBeatDAO = jdbiSensor.onDemand(PillHeartBeatDAO.class);
        final AmazonDynamoDB pillKeyStoreDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(configuration.getKeyStore().getEndpoint());
        final KeyStore pillKeyStore = new KeyStoreDynamoDB(pillKeyStoreDynamoDB,configuration.getKeyStore().getTableName(), new byte[16], 120);
        final IRecordProcessorFactory factory = new SavePillDataProcessorFactory(trackerMotionDAO, configuration.getBatchSize(), mergedUserInfoDynamoDB, heartBeatDAO, pillKeyStore, deviceDAO);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }
}

package com.hello.suripu.workers.pill;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;
import com.hello.suripu.workers.framework.WorkerEnvironmentCommand;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class PillWorkerCommand extends WorkerEnvironmentCommand<PillWorkerConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillWorkerCommand.class);

    public PillWorkerCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, PillWorkerConfiguration configuration) throws Exception {
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource sensorDataSource = managedDataSourceFactory.build(configuration.getSensorDB());


        final DBIFactory dbiFactory = new DBIFactory();
        final DBI sensorsDBI = dbiFactory.build(environment, configuration.getSensorDB(), "postgresql");
        final DBI commonDBI = dbiFactory.build(environment, configuration.getCommonDB(), "postgresql");

        // Joda Argument factory is not supported by default by DW, needs to be added manually
        sensorsDBI.registerArgumentFactory(new JodaArgumentFactory());
        commonDBI.registerArgumentFactory(new JodaArgumentFactory());

        final TrackerMotionDAO trackerMotionDAO = sensorsDBI.onDemand(TrackerMotionDAO.class);
        final DeviceDAO deviceDAO = commonDBI.onDemand(DeviceDAO.class);
        final PillHeartBeatDAO heartBeatDAO = commonDBI.onDemand(PillHeartBeatDAO.class);

        final ImmutableMap<QueueName, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueName.BATCH_PILL_DATA);
        LOGGER.info("\n\n\n!!! This worker is using the following queue: {} !!!\n\n\n", queueName);



        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";
            final String prefix = String.format("%s.%s.suripu-workers", apiKey, env);

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));
            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

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


        final ClientConfiguration clientConfiguration = new ClientConfiguration().withMaxErrorRetry(9); // 3x the default value
        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, clientConfiguration, configuration.dynamoDBConfiguration());
        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.FEATURES);
        final ImmutableMap<DynamoDBTableName, String> tableNames = configuration.dynamoDBConfiguration().tables();
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, tableNames.get(DynamoDBTableName.FEATURES), featureNamespace);

        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        final AmazonDynamoDB mergedUserInfoDynamoDBClient = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.ALARM_INFO);
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(mergedUserInfoDynamoDBClient, tableNames.get(DynamoDBTableName.ALARM_INFO));

        final AmazonDynamoDB pillKeyStoreDynamoDB = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.PILL_KEY_STORE);
        final KeyStore pillKeyStore = new KeyStoreDynamoDB(pillKeyStoreDynamoDB,tableNames.get(DynamoDBTableName.PILL_KEY_STORE), new byte[16], 120);
        final AmazonDynamoDB pillDynamoDB = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.PILL_HEARTBEAT);
        final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB = PillHeartBeatDAODynamoDB.create(pillDynamoDB, tableNames.get(DynamoDBTableName.PILL_HEARTBEAT));

        final IRecordProcessorFactory factory = new SavePillDataProcessorFactory(
                trackerMotionDAO,
                configuration.getBatchSize(),
                mergedUserInfoDynamoDB,
                heartBeatDAO,
                pillKeyStore,
                deviceDAO,
                pillHeartBeatDAODynamoDB
        );
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }
}

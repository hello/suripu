package com.hello.suripu.workers.sense;

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
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.workers.framework.WorkerEnvironmentCommand;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class SenseSaveWorkerCommand extends WorkerEnvironmentCommand<SenseSaveWorkerConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseSaveWorkerCommand.class);

    public SenseSaveWorkerCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, SenseSaveWorkerConfiguration configuration) throws Exception {

        final DBIFactory dbiFactory = new DBIFactory();
        final DBI commonDBI = dbiFactory.build(environment, configuration.getCommonDB(), "postgresql");
        final DBI sensorsDBI = dbiFactory.build(environment, configuration.getSensorsDB(), "postgresql");

        sensorsDBI.registerArgumentFactory(new JodaArgumentFactory());
        commonDBI.registerArgumentFactory(new JodaArgumentFactory());

        final DeviceDAO deviceDAO = commonDBI.onDemand(DeviceDAO.class);
        final DeviceDataDAO deviceDataDAO = sensorsDBI.onDemand(DeviceDataDAO.class);


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

        final ImmutableMap<QueueName, String> queueNames = configuration.getQueues();

        LOGGER.debug("{}", queueNames);
        final String queueName = queueNames.get(QueueName.SENSE_SENSORS_DATA);
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

        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());


        final AmazonDynamoDB alarmInfoDynamoDBClient = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.ALARM_INFO);
        final AmazonDynamoDB sensorViewsDynamoDBClient = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.SENSE_LAST_SEEN);
        final ImmutableMap<DynamoDBTableName, String> tableNames = configuration.dynamoDBConfiguration().tables();

        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.FEATURES);
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, tableNames.get(DynamoDBTableName.FEATURES), featureNamespace);

        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(alarmInfoDynamoDBClient , tableNames.get(DynamoDBTableName.ALARM_INFO));

        final SensorsViewsDynamoDB sensorsViewsDynamoDB = new SensorsViewsDynamoDB(
                sensorViewsDynamoDBClient,
                tableNames.get(DynamoDBTableName.SENSE_PREFIX),
                tableNames.get(DynamoDBTableName.SENSE_LAST_SEEN)
        );

        final JedisPool jedisPool = new JedisPool(
                configuration.getRedisConfiguration().getHost(),
                configuration.getRedisConfiguration().getPort()
        );

        final IRecordProcessorFactory factory = new SenseSaveProcessorFactory(
                deviceDAO,
                mergedUserInfoDynamoDB,
                sensorsViewsDynamoDB,
                deviceDataDAO,
                jedisPool
        );

        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }
}
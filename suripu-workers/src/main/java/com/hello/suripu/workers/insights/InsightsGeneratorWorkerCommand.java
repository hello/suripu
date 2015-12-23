package com.hello.suripu.workers.insights;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.coredw.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.workers.framework.WorkerEnvironmentCommand;
import com.hello.suripu.workers.framework.WorkerRolloutModule;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by kingshy on 1/6/15.
 */
public class InsightsGeneratorWorkerCommand extends WorkerEnvironmentCommand<InsightsGeneratorWorkerConfiguration> {
    private final static Logger LOGGER = LoggerFactory.getLogger(InsightsGeneratorWorkerCommand.class);

    public InsightsGeneratorWorkerCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, InsightsGeneratorWorkerConfiguration configuration) throws Exception {
        // postgres setup
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource commonDBDataSource = managedDataSourceFactory.build(configuration.getCommonDB());

        final DBI commonDBI = new DBI(commonDBDataSource);
        commonDBI.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
        commonDBI.registerContainerFactory(new ImmutableListContainerFactory());
        commonDBI.registerContainerFactory(new ImmutableSetContainerFactory());
        commonDBI.registerContainerFactory(new OptionalContainerFactory());
        commonDBI.registerArgumentFactory(new JodaArgumentFactory());

        final AccountDAO accountDAO = commonDBI.onDemand(AccountDAOImpl.class);
        final DeviceDAO deviceDAO = commonDBI.onDemand(DeviceDAO.class);

        final ManagedDataSource sensorDataSource = managedDataSourceFactory.build(configuration.getSensorsDB());
        final DBI sensorDBI = new DBI(sensorDataSource);
        sensorDBI.registerArgumentFactory(new OptionalArgumentFactory(configuration.getSensorsDB().getDriverClass()));
        sensorDBI.registerContainerFactory(new ImmutableListContainerFactory());
        sensorDBI.registerContainerFactory(new ImmutableSetContainerFactory());
        sensorDBI.registerContainerFactory(new OptionalContainerFactory());
        sensorDBI.registerArgumentFactory(new JodaArgumentFactory());


        final DeviceDataDAO deviceDataDAO = sensorDBI.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorDBI.onDemand(TrackerMotionDAO.class);

        final ManagedDataSource insightsDataSource = managedDataSourceFactory.build(configuration.getInsightsDB());
        final DBI insightsDBI = new DBI(insightsDataSource);
        insightsDBI.registerArgumentFactory(new OptionalArgumentFactory(configuration.getInsightsDB().getDriverClass()));
        insightsDBI.registerContainerFactory(new ImmutableListContainerFactory());
        insightsDBI.registerContainerFactory(new ImmutableSetContainerFactory());
        insightsDBI.registerContainerFactory(new OptionalContainerFactory());
        insightsDBI.registerArgumentFactory(new JodaArgumentFactory());

        final TrendsInsightsDAO trendsInsightsDAO = insightsDBI.onDemand(TrendsInsightsDAO.class);
        final QuestionResponseReadDAO questionResponseDAO = insightsDBI.onDemand(QuestionResponseReadDAO.class);

        // metrics stuff
        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";
            final String hostName = InetAddress.getLocalHost().getHostName();

            final String prefix = String.format("%s.%s.%s", apiKey, env, hostName);

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));
            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        // setup kinesis queue
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

        // setup dynamoDB clients
        final ImmutableMap<DynamoDBTableName, String> tableNames = configuration.dynamoDBConfiguration().tables();
        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());

        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.FEATURES);
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, tableNames.get(DynamoDBTableName.FEATURES), featureNamespace);

        final AmazonDynamoDB insightsDynamoDB = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.INSIGHTS);
        final InsightsDAODynamoDB insightsDAODynamoDB = new InsightsDAODynamoDB(insightsDynamoDB,
                tableNames.get(DynamoDBTableName.INSIGHTS));

        final AmazonDynamoDB accountPreferencesDynamoDBClient = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.PREFERENCES);
        final AccountPreferencesDAO accountPreferencesDynamoDB = AccountPreferencesDynamoDB.create(accountPreferencesDynamoDBClient,
                tableNames.get(DynamoDBTableName.PREFERENCES));

        final AmazonDynamoDB dynamoDBScoreClient = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.SLEEP_SCORE);
        final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                dynamoDBScoreClient,
                tableNames.get(DynamoDBTableName.SLEEP_SCORE),
                configuration.getSleepScoreVersion()
        );

        final AmazonDynamoDB dynamoDBStatsClient = amazonDynamoDBClientFactory.getForTable(DynamoDBTableName.SLEEP_STATS);
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(dynamoDBStatsClient,
                tableNames.get(DynamoDBTableName.SLEEP_STATS),
                configuration.getSleepStatsVersion());


        final AmazonDynamoDB calibrationDynamoDBClient = amazonDynamoDBClientFactory.getInstrumented(DynamoDBTableName.CALIBRATION, CalibrationDynamoDB.class);
        final CalibrationDAO calibrationDAO = CalibrationDynamoDB.create(
                calibrationDynamoDBClient,
                tableNames.get(DynamoDBTableName.CALIBRATION)
        );

        final AmazonDynamoDB deviceDataDAODynamoDBClient = amazonDynamoDBClientFactory.getInstrumented(DynamoDBTableName.DEVICE_DATA, DeviceDataDAODynamoDB.class);
        final DeviceDataDAODynamoDB deviceDataDAODynamoDB = new DeviceDataDAODynamoDB(deviceDataDAODynamoDBClient, tableNames.get(DynamoDBTableName.DEVICE_DATA));

        final AmazonDynamoDB timeZoneHistoryDAODynamoDBClient = amazonDynamoDBClientFactory.getInstrumented(DynamoDBTableName.TIMEZONE_HISTORY, TimeZoneHistoryDAODynamoDB.class);
        final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(timeZoneHistoryDAODynamoDBClient, tableNames.get(DynamoDBTableName.TIMEZONE_HISTORY));

        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        // external data for insights computation
        final LightData lightData = new LightData(); // lights global distribution
        final WakeStdDevData wakeStdDevData = new WakeStdDevData();

        final IRecordProcessorFactory factory = new InsightsGeneratorFactory(
                accountDAO,
                deviceDataDAO,
                deviceDataDAODynamoDB,
                deviceDAO,
                timeZoneHistoryDAODynamoDB,
                trackerMotionDAO,
                aggregateSleepScoreDAODynamoDB,
                insightsDAODynamoDB,
                trendsInsightsDAO,
                questionResponseDAO,
                sleepStatsDAODynamoDB,
                lightData,
                wakeStdDevData,
                accountPreferencesDynamoDB,
                calibrationDAO);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }

}

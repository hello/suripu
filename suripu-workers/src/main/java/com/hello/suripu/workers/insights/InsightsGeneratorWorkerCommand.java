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
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.*;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
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
        final QuestionResponseDAO questionResponseDAO = insightsDBI.onDemand(QuestionResponseDAO.class);

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
        final AmazonDynamoDBClientFactory amazonDynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB featureDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String featureNamespace = (configuration.getDebug()) ? "dev" : "prod";
        final FeatureStore featureStore = new FeatureStore(featureDynamoDB, "features", featureNamespace);

        final AmazonDynamoDB insightsDynamoDB = amazonDynamoDBClientFactory.getForEndpoint(configuration.getInsightsDynamoDB().getEndpoint());
        final InsightsDAODynamoDB insightsDAODynamoDB = new InsightsDAODynamoDB(insightsDynamoDB, configuration.getInsightsDynamoDB().getTableName());

        final AmazonDynamoDB accountPreferencesDynamoDBClient = amazonDynamoDBClientFactory.getForEndpoint(configuration.getInsightsDynamoDB().getEndpoint());
        final AccountPreferencesDAO accountPreferencesDynamoDB = AccountPreferencesDynamoDB.create(accountPreferencesDynamoDBClient, configuration.getPreferencesDynamoDB().getTableName());

        final AmazonDynamoDBClient dynamoDBScoreClient = new AmazonDynamoDBClient(awsCredentialsProvider);
        dynamoDBScoreClient.setEndpoint(configuration.getSleepScoreDynamoDB().getEndpoint());
        final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                dynamoDBScoreClient,
                configuration.getSleepScoreDynamoDB().getTableName(),
                configuration.getSleepScoreVersion()
        );

        final AmazonDynamoDB dynamoDBStatsClient = amazonDynamoDBClientFactory.getForEndpoint(configuration.getSleepStatsDynamoConfiguration().getEndpoint());
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(dynamoDBStatsClient,
                configuration.getSleepStatsDynamoConfiguration().getTableName(),
                configuration.getSleepStatsVersion());


        final WorkerRolloutModule workerRolloutModule = new WorkerRolloutModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(workerRolloutModule);

        // external data for insights computation
        final LightData lightData = new LightData(); // lights global distribution
        final WakeStdDevData wakeStdDevData = new WakeStdDevData();

        //Instantiate timelineProcessor
        final DBI commonDB = new DBI(managedDataSourceFactory.build(configuration.getCommonDB()));
        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);
        final AmazonDynamoDB sleepHmmDynamoDbClient = dynamoDBClientFactory.getForEndpoint(
                configuration.getDynamoDBConfiguration().endpoints().get(DynamoDBTableName.SLEEP_HMM));
        final String sleepHmmTableName = configuration.getDynamoDBConfiguration().tables().get(DynamoDBTableName.SLEEP_HMM);
        final SleepHmmDAODynamoDB sleepHmmDAODynamoDB = new SleepHmmDAODynamoDB(sleepHmmDynamoDbClient,sleepHmmTableName);
        final AmazonDynamoDB ringTimeDynamoDBClient = dynamoDBClientFactory.getForEndpoint(
                configuration.getDynamoDBConfiguration().endpoints().get(DynamoDBTableName.RING_TIME_HISTORY));
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(
                ringTimeDynamoDBClient,
                configuration.getDynamoDBConfiguration().tables().get(DynamoDBTableName.RING_TIME_HISTORY));
        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
        final TimelineProcessor timelineProcessor =
                TimelineProcessor.createTimelineProcessor(trackerMotionDAO,
                        deviceDAO, deviceDataDAO,
                        ringTimeHistoryDAODynamoDB,
                        feedbackDAO,
                        sleepHmmDAODynamoDB,
                        accountDAO,
                        sleepStatsDAODynamoDB,
                        senseColorDAO);

        final IRecordProcessorFactory factory = new InsightsGeneratorFactory(
                accountDAO,
                deviceDataDAO,
                deviceDAO,
                trackerMotionDAO,
                aggregateSleepScoreDAODynamoDB,
                insightsDAODynamoDB,
                trendsInsightsDAO,
                questionResponseDAO,
                sleepStatsDAODynamoDB,
                lightData,
                wakeStdDevData,
                timelineProcessor,
                accountPreferencesDynamoDB);
        final Worker worker = new Worker(factory, kinesisConfig);
        worker.run();
    }

}

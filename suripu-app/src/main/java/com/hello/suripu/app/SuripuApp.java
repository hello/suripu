package com.hello.suripu.app;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.app.cli.CreateDynamoDBTables;
import com.hello.suripu.app.cli.PopulateSleepScoreTable;
import com.hello.suripu.app.cli.RecreatePillColorCommand;
import com.hello.suripu.app.cli.ScanInvalidNightsCommand;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.modules.RolloutAppModule;
import com.hello.suripu.app.resources.v1.AccountPreferencesResource;
import com.hello.suripu.app.resources.v1.AccountResource;
import com.hello.suripu.app.resources.v1.AlarmResource;
import com.hello.suripu.app.resources.v1.AppCheckinResource;
import com.hello.suripu.app.resources.v1.DeviceResources;
import com.hello.suripu.app.resources.v1.FeedbackResource;
import com.hello.suripu.app.resources.v1.InsightsResource;
import com.hello.suripu.app.resources.v1.MobilePushRegistrationResource;
import com.hello.suripu.app.resources.v1.OAuthResource;
import com.hello.suripu.app.resources.v1.PasswordResetResource;
import com.hello.suripu.app.resources.v1.ProvisionResource;
import com.hello.suripu.app.resources.v1.QuestionsResource;
import com.hello.suripu.app.resources.v1.RoomConditionsResource;
import com.hello.suripu.app.resources.v1.SupportResource;
import com.hello.suripu.app.resources.v1.TimeZoneResource;
import com.hello.suripu.app.resources.v1.TimelineResource;
import com.hello.suripu.app.v2.StoreFeedbackResource;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.BayesNetHmmModelDAODynamoDB;
import com.hello.suripu.core.db.BayesNetHmmModelPriorsDAO;
import com.hello.suripu.core.db.BayesNetHmmModelPriorsDAODynamoDB;
import com.hello.suripu.core.db.BayesNetModelDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.SleepHmmDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TimelineLogDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.filters.CacheFilterFactory;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import com.hello.suripu.core.notifications.NotificationSubscriptionDAOWrapper;
import com.hello.suripu.core.notifications.NotificationSubscriptionsDAO;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.core.provision.PillProvisionDAO;
import com.hello.suripu.core.store.StoreFeedbackDAO;
import com.hello.suripu.core.support.SupportDAO;
import com.hello.suripu.core.util.CustomJSONExceptionMapper;
import com.hello.suripu.core.util.DropwizardServiceUtil;
import com.hello.suripu.core.util.KeyStoreUtils;
import com.hello.suripu.coredw.db.TimelineDAODynamoDB;
import com.hello.suripu.coredw.db.TimelineLogDAODynamoDB;
import com.hello.suripu.coredw.oauth.OAuthAuthenticator;
import com.hello.suripu.coredw.oauth.OAuthProvider;
import com.sun.jersey.api.core.ResourceConfig;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SuripuApp extends Service<SuripuAppConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuripuApp.class);

    public static void main(final String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new SuripuApp().run(args);
    }

    @Override
    public void initialize(final Bootstrap<SuripuAppConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addCommand(new CreateDynamoDBTables());
        bootstrap.addCommand(new RecreatePillColorCommand());
        bootstrap.addCommand(new PopulateSleepScoreTable());
        bootstrap.addCommand(new ScanInvalidNightsCommand());
    }

    @Override
    public void run(final SuripuAppConfiguration configuration, final Environment environment) throws Exception {

        final DBIFactory factory = new DBIFactory();
        final DBI sensorsDB = factory.build(environment, configuration.getSensorsDB(), "postgresql");
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql");
        final DBI insightsDB = factory.build(environment, configuration.getInsightsDB(), "postgresql");

        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        
        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());

        insightsDB.registerArgumentFactory(new JodaArgumentFactory());
        insightsDB.registerContainerFactory(new OptionalContainerFactory());
        insightsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());


        // COMMON DB
        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final PillProvisionDAO pillProvisionDAO = commonDB.onDemand(PillProvisionDAO.class);
        final PillHeartBeatDAO pillHeartBeatDAO = commonDB.onDemand(PillHeartBeatDAO.class);
        final SupportDAO supportDAO = commonDB.onDemand(SupportDAO.class);
        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);

        // SENSORS DB
        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);

        // TODO: replace this with commonDB pool to decrease # of connections to common db host
        // INSIGHTS DB
        final QuestionResponseDAO questionResponseDAO = insightsDB.onDemand(QuestionResponseDAO.class);
        final TrendsInsightsDAO trendsInsightsDAO = insightsDB.onDemand(TrendsInsightsDAO.class);

        final NotificationSubscriptionsDAO notificationSubscriptionsDAO = commonDB.onDemand(NotificationSubscriptionsDAO.class);

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);
        final PersistentAccessTokenStore accessTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        // AWS SDK
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.withConnectionTimeout(200); // in ms
        clientConfiguration.withMaxErrorRetry(1);

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonSNSClient snsClient = new AmazonSNSClient(awsCredentialsProvider, clientConfiguration);
        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider, clientConfiguration);
        final AmazonS3 amazonS3 = new AmazonS3Client(awsCredentialsProvider, clientConfiguration);

        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());


        final AlarmDAODynamoDB alarmDAODynamoDB = (AlarmDAODynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.ALARMS);
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = (MergedUserInfoDynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.ALARM_INFO);
        final BayesNetModelDAO modelDAO = (BayesNetHmmModelDAODynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.BAYESNET_MODEL);
        final BayesNetHmmModelPriorsDAO priorsDAO = (BayesNetHmmModelPriorsDAODynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.BAYESNET_PRIORS);
        final CalibrationDAO calibrationDAO = (CalibrationDynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.CALIBRATION);
        final InsightsDAODynamoDB insightsDAODynamoDB = (InsightsDAODynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.INSIGHTS);
        final PasswordResetDB passwordResetDB = (PasswordResetDB) dynamoDBClientFactory.get(DynamoDBTableName.PASSWORD_RESET);
        final AccountPreferencesDAO accountPreferencesDAO = (AccountPreferencesDAO) dynamoDBClientFactory.get(DynamoDBTableName.PREFERENCES);
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = (RingTimeHistoryDAODynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.RING_TIME_HISTORY);
        final SensorsViewsDynamoDB sensorsViewsDynamoDB = (SensorsViewsDynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.SENSE_LAST_SEEN);
        final SleepHmmDAODynamoDB sleepHmmDAODynamoDB = (SleepHmmDAODynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.SLEEP_HMM);
        final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = (TimeZoneHistoryDAODynamoDB) dynamoDBClientFactory.get(DynamoDBTableName.TIMEZONE_HISTORY);


        // WARNING: Due to module conflicts the following two should be handled manually
        final AmazonDynamoDB timelineDAOClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.TIMELINE_LOG);
        final TimelineDAODynamoDB timelineDAODynamoDB = new TimelineDAODynamoDB(
                timelineDAOClient, configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.TIMELINE),
                configuration.getMaxCacheRefreshDay()
        );

        final AmazonDynamoDB timelineLogClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.TIMELINE_LOG);
        final TimelineLogDAO timelineLogDAO = new TimelineLogDAODynamoDB(
                timelineLogClient,
                configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.TIMELINE_LOG)
        );

        // Exceptions because constructor has an extra parameter
        final AmazonDynamoDB dynamoDBScoreClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SLEEP_SCORE);
        final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                dynamoDBScoreClient,
                configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SLEEP_SCORE),
                configuration.getSleepScoreVersion()
        );

        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SLEEP_STATS);
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(dynamoDBStatsClient,
                configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SLEEP_STATS),
                configuration.getSleepStatsVersion());



        // KEYSTORES
        final AmazonDynamoDB senseKeyStoreDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.SENSE_KEY_STORE);
        final KeyStore senseKeyStore = new KeyStoreDynamoDB(
                senseKeyStoreDynamoDBClient,
                configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SENSE_KEY_STORE),
                "1234567891234567".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );

        final AmazonDynamoDB pillKeyStoreDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.PILL_KEY_STORE);
        final KeyStore pillKeyStore = new KeyStoreDynamoDB(
                pillKeyStoreDynamoDBClient,
                configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.PILL_KEY_STORE),
                "9876543219876543".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );


        final ImmutableMap<String, String> arns = ImmutableMap.copyOf(configuration.getPushNotificationsConfiguration().getArns());
        final NotificationSubscriptionDAOWrapper notificationSubscriptionDAOWrapper = NotificationSubscriptionDAOWrapper.create(
                notificationSubscriptionsDAO,
                snsClient,
                arns
        );

        final MobilePushNotificationProcessor mobilePushNotificationProcessor = new MobilePushNotificationProcessor(snsClient, notificationSubscriptionsDAO);

        final ImmutableMap<QueueName, String> streams = ImmutableMap.copyOf(configuration.getKinesisConfiguration().getStreams());
        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(kinesisClient, streams);
        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);
        final DataLogger timelineLogger = kinesisLoggerFactory.get(QueueName.LOGS);

        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";

            final String prefix = String.format("%s.%s.%s", apiKey, env, "suripu-app");

            final List<String> metrics = configuration.getGraphite().getIncludeMetrics();
            final RegexMetricPredicate predicate = new RegexMetricPredicate(metrics);
            final Joiner joiner = Joiner.on(", ");
            LOGGER.info("Logging the following metrics: {}", joiner.join(metrics));

            GraphiteReporter.enable(Metrics.defaultRegistry(), interval, TimeUnit.SECONDS, graphiteHostName, 2003, prefix, predicate);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        LOGGER.warn("DEBUG MODE = {}", configuration.getDebug());
        // Custom JSON handling for responses.
        final ResourceConfig jrConfig = environment.getJerseyResourceConfig();
        DropwizardServiceUtil.deregisterDWSingletons(jrConfig);
        environment.addProvider(new CustomJSONExceptionMapper(configuration.getDebug()));

        environment.addProvider(new OAuthProvider(new OAuthAuthenticator(accessTokenStore), "protected-resources", activityLogger));

        environment.getJerseyResourceConfig()
                .getResourceFilterFactories().add(CacheFilterFactory.class);

        final String namespace = (configuration.getDebug()) ? "dev" : "prod";
        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getForTable(DynamoDBTableName.FEATURES);
        final FeatureStore featureStore = new FeatureStore(featuresDynamoDBClient, "features", namespace); // TODO: remove hardcoded table name


        final RolloutAppModule module = new RolloutAppModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);


        environment.addResource(new OAuthResource(accessTokenStore, applicationStore, accountDAO, notificationSubscriptionDAOWrapper));
        environment.addResource(new AccountResource(accountDAO));
        environment.addProvider(new RoomConditionsResource(accountDAO, deviceDataDAO, deviceDAO, configuration.getAllowedQueryRange(),senseColorDAO, calibrationDAO));
        environment.addResource(new DeviceResources(deviceDAO, deviceDataDAO, trackerMotionDAO, mergedUserInfoDynamoDB, pillHeartBeatDAO, sensorsViewsDynamoDB));

        final KeyStoreUtils keyStoreUtils = KeyStoreUtils.build(amazonS3, "hello-secure", "hello-pvt.pem");
        environment.addResource(new ProvisionResource(senseKeyStore, pillKeyStore, keyStoreUtils, pillProvisionDAO, amazonS3));

        final TimelineProcessor timelineProcessor = TimelineProcessor.createTimelineProcessor(
                trackerMotionDAO,
                deviceDAO,
                deviceDataDAO,
                ringTimeHistoryDAODynamoDB,
                feedbackDAO,
                sleepHmmDAODynamoDB,
                accountDAO,
                sleepStatsDAODynamoDB,
                senseColorDAO,
                priorsDAO,
                modelDAO,
                calibrationDAO);

        environment.addResource(new TimelineResource(accountDAO, timelineDAODynamoDB, timelineLogDAO,timelineLogger, timelineProcessor));

        environment.addResource(new TimeZoneResource(timeZoneHistoryDAODynamoDB, mergedUserInfoDynamoDB, deviceDAO));
        environment.addResource(new AlarmResource(alarmDAODynamoDB, mergedUserInfoDynamoDB, deviceDAO, amazonS3));

        environment.addResource(new MobilePushRegistrationResource(notificationSubscriptionDAOWrapper, mobilePushNotificationProcessor, accountDAO));

        environment.addResource(new QuestionsResource(accountDAO, questionResponseDAO, timeZoneHistoryDAODynamoDB, configuration.getQuestionConfigs().getNumSkips()));
        environment.addResource(new FeedbackResource(feedbackDAO, timelineDAODynamoDB));
        environment.addResource(new AppCheckinResource(2015000000));

        // data science resource stuff
        final AccountInfoProcessor.Builder builder = new AccountInfoProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withMapping(questionResponseDAO);
        final AccountInfoProcessor accountInfoProcessor = builder.build();


        environment.addResource(new AccountPreferencesResource(accountPreferencesDAO));

        final InsightProcessor.Builder insightBuilder = new InsightProcessor.Builder()
                .withSenseDAOs(deviceDataDAO, deviceDAO)
                .withTrackerMotionDAO(trackerMotionDAO)
                .withInsightsDAO(trendsInsightsDAO)
                .withDynamoDBDAOs(aggregateSleepScoreDAODynamoDB, insightsDAODynamoDB, sleepStatsDAODynamoDB)
                .withPreferencesDAO(accountPreferencesDAO)
                .withAccountInfoProcessor(accountInfoProcessor)
                .withWakeStdDevData(new WakeStdDevData())
                .withLightData(new LightData());
        final InsightProcessor insightProcessor = insightBuilder.build();

        environment.addResource(new InsightsResource(accountDAO, trendsInsightsDAO, aggregateSleepScoreDAODynamoDB, trackerMotionDAO, insightsDAODynamoDB, sleepStatsDAODynamoDB, insightProcessor));

        LOGGER.debug("{}", DateTime.now(DateTimeZone.UTC).getMillis());

        if(configuration.getDebug()) {
            environment.addResource(new VersionResource());
            environment.addResource(new PingResource());
        }


        environment.addResource(PasswordResetResource.create(accountDAO, passwordResetDB, configuration.emailConfiguration()));

        environment.addResource(new SupportResource(supportDAO));
        environment.addResource(new com.hello.suripu.app.v2.TimelineResource(timelineDAODynamoDB, timelineProcessor, timelineLogDAO, feedbackDAO, trackerMotionDAO, sleepStatsDAODynamoDB,timelineLogger));
        environment.addResource(new com.hello.suripu.app.v2.AccountPreferencesResource(accountPreferencesDAO));
        StoreFeedbackDAO storeFeedbackDAO = commonDB.onDemand(StoreFeedbackDAO.class);
        environment.addResource(new StoreFeedbackResource(storeFeedbackDAO));
    }
}

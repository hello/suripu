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
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.BayesNetHmmModelDAODynamoDB;
import com.hello.suripu.core.db.BayesNetModelDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.BayesNetHmmModelPriorsDAO;
import com.hello.suripu.core.db.BayesNetHmmModelPriorsDAODynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.store.StoreFeedbackDAO;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.coredw.db.SleepHmmDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TeamStoreDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.coredw.db.TimelineDAODynamoDB;
import com.hello.suripu.core.db.TimelineLogDAO;
import com.hello.suripu.coredw.db.TimelineLogDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.db.UserLabelDAO;
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
import com.hello.suripu.coredw.oauth.OAuthAuthenticator;
import com.hello.suripu.coredw.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.provision.PillProvisionDAO;
import com.hello.suripu.core.support.SupportDAO;
import com.hello.suripu.core.util.CustomJSONExceptionMapper;
import com.hello.suripu.core.util.DropwizardServiceUtil;
import com.hello.suripu.core.util.KeyStoreUtils;
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

        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final PillProvisionDAO pillProvisionDAO = commonDB.onDemand(PillProvisionDAO.class);

        final UserLabelDAO userLabelDAO = commonDB.onDemand(UserLabelDAO.class);
        final TrendsInsightsDAO trendsInsightsDAO = insightsDB.onDemand(TrendsInsightsDAO.class);
        final PillHeartBeatDAO pillHeartBeatDAO = commonDB.onDemand(PillHeartBeatDAO.class);
        final SupportDAO supportDAO = commonDB.onDemand(SupportDAO.class);

        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);
        final QuestionResponseDAO questionResponseDAO = insightsDB.onDemand(QuestionResponseDAO.class);
        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);
        final NotificationSubscriptionsDAO notificationSubscriptionsDAO = commonDB.onDemand(NotificationSubscriptionsDAO.class);

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);
        final PersistentAccessTokenStore accessTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.withConnectionTimeout(200); // in ms
        clientConfiguration.withMaxErrorRetry(1);

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory dynamoDBClientFactory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider);



        final AmazonSNSClient snsClient = new AmazonSNSClient(awsCredentialsProvider, clientConfiguration);
        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider, clientConfiguration);

        final AmazonS3 amazonS3 = new AmazonS3Client(awsCredentialsProvider, clientConfiguration);

        final AmazonDynamoDB timelineDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getTimelineDBConfiguration().getEndpoint());
        final TimelineDAODynamoDB timelineDAODynamoDB = new TimelineDAODynamoDB(timelineDynamoDBClient,
                configuration.getTimelineDBConfiguration().getTableName(),
                configuration.getMaxCacheRefreshDay());

        final AmazonDynamoDB sleepHmmDynamoDbClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepHmmDBConfiguration().getEndpoint());
        final String sleepHmmTableName = configuration.getSleepHmmDBConfiguration().getTableName();
        final SleepHmmDAODynamoDB sleepHmmDAODynamoDB = new SleepHmmDAODynamoDB(sleepHmmDynamoDbClient,sleepHmmTableName);
        final AmazonDynamoDB alarmDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getAlarmDBConfiguration().getEndpoint());
        final AlarmDAODynamoDB alarmDAODynamoDB = new AlarmDAODynamoDB(
                alarmDynamoDBClient, configuration.getAlarmDBConfiguration().getTableName()
        );

        final AmazonDynamoDB timezoneHistoryDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getTimeZoneHistoryDBConfiguration().getEndpoint());
        final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(
                timezoneHistoryDynamoDBClient, configuration.getTimeZoneHistoryDBConfiguration().getTableName()
        );

        final AmazonDynamoDB mergedUserInfoDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getUserInfoDynamoDBConfiguration().getEndpoint());
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(
                mergedUserInfoDynamoDBClient, configuration.getUserInfoDynamoDBConfiguration().getTableName()
        );

        final AmazonDynamoDB insightsDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getInsightsDynamoDBConfiguration().getEndpoint());
        final InsightsDAODynamoDB insightsDAODynamoDB = new InsightsDAODynamoDB(
                insightsDynamoDBClient, configuration.getInsightsDynamoDBConfiguration().getTableName());

        final AmazonDynamoDB dynamoDBScoreClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepScoreDBConfiguration().getEndpoint());

        final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                dynamoDBScoreClient,
                configuration.getSleepScoreDBConfiguration().getTableName(),
                configuration.getSleepScoreVersion()
        );

        final AmazonDynamoDB dynamoDBStatsClient = dynamoDBClientFactory.getForEndpoint(configuration.getSleepStatsDynamoConfiguration().getEndpoint());
        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = new SleepStatsDAODynamoDB(dynamoDBStatsClient,
                configuration.getSleepStatsDynamoConfiguration().getTableName(),
                configuration.getSleepStatsVersion());

        final ImmutableMap<String, String> arns = ImmutableMap.copyOf(configuration.getPushNotificationsConfiguration().getArns());

        final AmazonDynamoDB ringTimeHistoryDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getRingTimeHistoryDBConfiguration().getEndpoint());
        final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(ringTimeHistoryDynamoDBClient,
                configuration.getRingTimeHistoryDBConfiguration().getTableName());

        final NotificationSubscriptionDAOWrapper notificationSubscriptionDAOWrapper = NotificationSubscriptionDAOWrapper.create(
                notificationSubscriptionsDAO,
                snsClient,
                arns
        );

        final MobilePushNotificationProcessor mobilePushNotificationProcessor = new MobilePushNotificationProcessor(snsClient, notificationSubscriptionsDAO);

        /*  Timeline Log dynamo dB stuff */
        final String timelineLogTableName = configuration.getTimelineLogDBConfiguration().getTableName();
        final AmazonDynamoDB timelineLogDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getTimelineLogDBConfiguration().getEndpoint());
        final TimelineLogDAO timelineLogDAO = new TimelineLogDAODynamoDB(timelineLogDynamoDBClient,timelineLogTableName);

        /* Priors for bayesnet  */
        final String priorDbTableName = configuration.getHmmBayesnetPriorsConfiguration().getTableName();
        final AmazonDynamoDB priorsDb = dynamoDBClientFactory.getForEndpoint(configuration.getHmmBayesnetPriorsConfiguration().getEndpoint());
        final BayesNetHmmModelPriorsDAO priorsDAO = new BayesNetHmmModelPriorsDAODynamoDB(priorsDb,priorDbTableName);

        /* Models for bayesnet */
        final String modelDbTableName = configuration.getHmmBayesnetModelsConfiguration().getTableName();
        final AmazonDynamoDB modelsDb = dynamoDBClientFactory.getForEndpoint(configuration.getHmmBayesnetModelsConfiguration().getEndpoint());
        final BayesNetModelDAO modelDAO = new BayesNetHmmModelDAODynamoDB(modelsDb,modelDbTableName);

        final AmazonDynamoDB teamStoreDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getTeamsDynamoDBConfiguration().getEndpoint());
        final TeamStoreDAO teamStore = new TeamStore(teamStoreDBClient, "teams");

        final ImmutableMap<QueueName, String> streams = ImmutableMap.copyOf(configuration.getKinesisConfiguration().getStreams());
        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(kinesisClient, streams);
        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);

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
        final AmazonDynamoDB featuresDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final FeatureStore featureStore = new FeatureStore(featuresDynamoDBClient, "features", namespace);



        final RolloutAppModule module = new RolloutAppModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);

        final AmazonDynamoDB senseKeyStoreDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getSenseKeyStoreDynamoDBConfiguration().getEndpoint());
        final KeyStore senseKeyStore = new KeyStoreDynamoDB(
                senseKeyStoreDynamoDBClient,
                configuration.getSenseKeyStoreDynamoDBConfiguration().getTableName(),
                "1234567891234567".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );

        final AmazonDynamoDB pillKeyStoreDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getPillKeyStoreDynamoDBConfiguration().getEndpoint());
        final KeyStore pillKeyStore = new KeyStoreDynamoDB(
                pillKeyStoreDynamoDBClient,
                configuration.getPillKeyStoreDynamoDBConfiguration().getTableName(),
                "9876543219876543".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );


        final SenseColorDAO senseColorDAO = commonDB.onDemand(SenseColorDAOSQLImpl.class);
        
        final AmazonDynamoDB senseLastSeenDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getSenseLastSeenConfiguration().getEndpoint());
        final SensorsViewsDynamoDB sensorsViewsDynamoDB = new SensorsViewsDynamoDB(
                senseLastSeenDynamoDBClient,
                "", // We are not using dynamodb for minute by minute data yet
                configuration.getSenseLastSeenConfiguration().getTableName()
        );



        environment.addResource(new OAuthResource(accessTokenStore, applicationStore, accountDAO, notificationSubscriptionDAOWrapper));
        environment.addResource(new AccountResource(accountDAO));
        environment.addProvider(new RoomConditionsResource(accountDAO, deviceDataDAO, deviceDAO, configuration.getAllowedQueryRange(),senseColorDAO));
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
                modelDAO);

        environment.addResource(new TimelineResource(accountDAO, timelineDAODynamoDB, timelineLogDAO, timelineProcessor));

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

        final AmazonDynamoDB prefsClient = dynamoDBClientFactory.getForEndpoint(configuration.getPreferencesDBConfiguration().getEndpoint());
        final AccountPreferencesDAO accountPreferencesDAO = AccountPreferencesDynamoDB.create(prefsClient, configuration.getPreferencesDBConfiguration().getTableName());
        environment.addResource(new AccountPreferencesResource(accountPreferencesDAO));

        final InsightProcessor.Builder insightBuilder = new InsightProcessor.Builder()
                .withSenseDAOs(deviceDataDAO, deviceDAO)
                .withTrackerMotionDAO(trackerMotionDAO)
                .withInsightsDAO(trendsInsightsDAO)
                .withDynamoDBDAOs(aggregateSleepScoreDAODynamoDB, insightsDAODynamoDB)
                .withSleepStatsDAODynamoDB(sleepStatsDAODynamoDB)
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

        final AmazonDynamoDB passwordResetDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getPasswordResetDBConfiguration().getEndpoint());
        final PasswordResetDB passwordResetDB = PasswordResetDB.create(passwordResetDynamoDBClient, configuration.getPasswordResetDBConfiguration().getTableName());

        environment.addResource(PasswordResetResource.create(accountDAO, passwordResetDB, configuration.emailConfiguration()));

        environment.addResource(new SupportResource(supportDAO));
        environment.addResource(new com.hello.suripu.app.v2.TimelineResource(timelineDAODynamoDB, timelineProcessor, feedbackDAO, trackerMotionDAO, sleepStatsDAODynamoDB));
        environment.addResource(new com.hello.suripu.app.v2.AccountPreferencesResource(accountPreferencesDAO));
        StoreFeedbackDAO storeFeedbackDAO = commonDB.onDemand(StoreFeedbackDAO.class);
        environment.addResource(new StoreFeedbackResource(storeFeedbackDAO));
    }
}

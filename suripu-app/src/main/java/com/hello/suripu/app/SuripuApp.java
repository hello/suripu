package com.hello.suripu.app;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.app.cli.CreateDynamoDBTables;
import com.hello.suripu.app.cli.MigrateDeviceDataCommand;
import com.hello.suripu.app.cli.MigratePillHeartbeatCommand;
import com.hello.suripu.app.cli.MovePillDataToDynamoDBCommand;
import com.hello.suripu.app.cli.RecreatePillColorCommand;
import com.hello.suripu.app.cli.ScanInvalidNightsCommand;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.modules.RolloutAppModule;
import com.hello.suripu.app.resources.v1.AccountPreferencesResource;
import com.hello.suripu.app.resources.v1.AccountResource;
import com.hello.suripu.app.resources.v1.AlarmResource;
import com.hello.suripu.app.resources.v1.AppCheckinResource;
import com.hello.suripu.app.resources.v1.AppStatsResource;
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
import com.hello.suripu.app.v2.DeviceResource;
import com.hello.suripu.app.v2.StoreFeedbackResource;
import com.hello.suripu.app.v2.TrendsResource;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.AccountLocationDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.AppStatsDAO;
import com.hello.suripu.core.db.AppStatsDAODynamoDB;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleFromS3;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAODynamoDB;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAODynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TeamStoreDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TimelineLogDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.db.UserLabelDAO;
import com.hello.suripu.core.db.UserTimelineTestGroupDAO;
import com.hello.suripu.core.db.UserTimelineTestGroupDAOImpl;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.db.WifiInfoDynamoDB;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.db.colors.SenseColorDAOSQLImpl;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.filters.CacheFilterFactory;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.models.device.v2.DeviceProcessor;
import com.hello.suripu.core.trends.v2.TrendsProcessor;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import com.hello.suripu.core.notifications.NotificationSubscriptionDAOWrapper;
import com.hello.suripu.core.notifications.NotificationSubscriptionsDAO;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeatDAODynamoDB;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.processors.QuestionProcessor;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.provision.PillProvisionDAO;
import com.hello.suripu.core.store.StoreFeedbackDAO;
import com.hello.suripu.core.support.SupportDAO;
import com.hello.suripu.core.util.CustomJSONExceptionMapper;
import com.hello.suripu.core.util.DropwizardServiceUtil;
import com.hello.suripu.core.util.KeyStoreUtils;
import com.hello.suripu.coredw.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.coredw.configuration.S3BucketConfiguration;
import com.hello.suripu.coredw.db.SleepHmmDAODynamoDB;
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
        bootstrap.addCommand(new ScanInvalidNightsCommand());
        bootstrap.addCommand(new MigratePillHeartbeatCommand());
        bootstrap.addCommand(new MigrateDeviceDataCommand());
        bootstrap.addCommand(new MovePillDataToDynamoDBCommand());
    }

    @Override
    public void run(final SuripuAppConfiguration configuration, final Environment environment) throws Exception {

        final DBIFactory factory = new DBIFactory();
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql");
        final DBI insightsDB = factory.build(environment, configuration.getInsightsDB(), "postgresql");

        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());
        commonDB.registerContainerFactory(new ImmutableListContainerFactory());
        commonDB.registerContainerFactory(new ImmutableSetContainerFactory());

        insightsDB.registerArgumentFactory(new JodaArgumentFactory());
        insightsDB.registerContainerFactory(new OptionalContainerFactory());
        insightsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final AccountLocationDAO accountLocationDAO = commonDB.onDemand(AccountLocationDAO.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = commonDB.onDemand(DeviceDAO.class);
        final PillProvisionDAO pillProvisionDAO = commonDB.onDemand(PillProvisionDAO.class);
        final UserTimelineTestGroupDAO userTimelineTestGroupDAO = commonDB.onDemand(UserTimelineTestGroupDAOImpl.class);

        final UserLabelDAO userLabelDAO = commonDB.onDemand(UserLabelDAO.class);
        final TrendsInsightsDAO trendsInsightsDAO = insightsDB.onDemand(TrendsInsightsDAO.class);
        final PillHeartBeatDAO pillHeartBeatDAO = commonDB.onDemand(PillHeartBeatDAO.class);
        final SupportDAO supportDAO = commonDB.onDemand(SupportDAO.class);

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

        final AmazonDynamoDB appStatsDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getAppStatsConfiguration().getEndpoint());
        final AppStatsDAO appStatsDAO = new AppStatsDAODynamoDB(appStatsDynamoDBClient, configuration.getAppStatsConfiguration().getTableName());

        final AmazonDynamoDB deviceDataDAODynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        final DeviceDataDAODynamoDB deviceDataDAODynamoDB = new DeviceDataDAODynamoDB(deviceDataDAODynamoDBClient, configuration.getDeviceDataConfiguration().getTableName());

        final AmazonDynamoDB pillDataDAODynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        final PillDataDAODynamoDB pillDataDAODynamoDB = new PillDataDAODynamoDB(pillDataDAODynamoDBClient, configuration.getPillDataConfiguration().getTableName());

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

        /* Individual models for users  */
        final String onlineHmmModelsTableName = configuration.getOnlineHmmModelsConfiguration().getTableName();
        final AmazonDynamoDB onlineHmmModelsDb = dynamoDBClientFactory.getForEndpoint(configuration.getOnlineHmmModelsConfiguration().getEndpoint());
        final OnlineHmmModelsDAO onlineHmmModelsDAO = OnlineHmmModelsDAODynamoDB.create(onlineHmmModelsDb,onlineHmmModelsTableName);

        /* Models for feature extraction layer */
        final String featureExtractionModelsTableName = configuration.getFeatureExtractionModelsConfiguration().getTableName();
        final AmazonDynamoDB featureExtractionModelsDb = dynamoDBClientFactory.getForEndpoint(configuration.getFeatureExtractionModelsConfiguration().getEndpoint());
        final FeatureExtractionModelsDAO featureExtractionDAO = new FeatureExtractionModelsDAODynamoDB(featureExtractionModelsDb,featureExtractionModelsTableName);

        /* Default model ensemble for all users  */
        final S3BucketConfiguration timelineModelEnsemblesConfig = configuration.getTimelineModelEnsemblesConfiguration();
        final S3BucketConfiguration seedModelConfig = configuration.getTimelineSeedModelConfiguration();

        final DefaultModelEnsembleDAO defaultModelEnsembleDAO = DefaultModelEnsembleFromS3.create(amazonS3,timelineModelEnsemblesConfig.getBucket(),timelineModelEnsemblesConfig.getKey(),seedModelConfig.getBucket(),seedModelConfig.getKey());

        final AmazonDynamoDB teamStoreDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getTeamsDynamoDBConfiguration().getEndpoint());
        final TeamStoreDAO teamStore = new TeamStore(teamStoreDBClient, "teams");

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

        // WARNING: Do not use async methods for anything but SensorsViewsDynamoDB for now
        final AmazonDynamoDBAsync senseLastSeenDynamoDBClient = new AmazonDynamoDBAsyncClient(awsCredentialsProvider, AmazonDynamoDBClientFactory.getDefaultClientConfiguration());
        senseLastSeenDynamoDBClient.setEndpoint(configuration.getSenseLastSeenConfiguration().getEndpoint());

        final SensorsViewsDynamoDB sensorsViewsDynamoDB = new SensorsViewsDynamoDB(
                senseLastSeenDynamoDBClient,
                "", // We are not using dynamodb for minute by minute data yet
                configuration.getSenseLastSeenConfiguration().getTableName()
        );


        final AmazonDynamoDB calibrationDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getCalibrationConfiguration().getEndpoint());
        final CalibrationDAO calibrationDAO = CalibrationDynamoDB.create(calibrationDynamoDBClient, configuration.getCalibrationConfiguration().getTableName());

        final AmazonDynamoDB wifiInfoDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getWifiInfoConfiguration().getEndpoint());
        final WifiInfoDAO wifiInfoDAO = new WifiInfoDynamoDB(wifiInfoDynamoDBClient, configuration.getWifiInfoConfiguration().getTableName());

        // TODO: replace with interface not DAO once namespace don't conflict
        final AmazonDynamoDB pillHeartBeatDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getPillHeartBeatConfiguration().getEndpoint());
        final PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB = PillHeartBeatDAODynamoDB.create(pillHeartBeatDynamoDBClient, configuration.getPillHeartBeatConfiguration().getTableName());

        environment.addResource(new OAuthResource(accessTokenStore, applicationStore, accountDAO, notificationSubscriptionDAOWrapper));
        environment.addResource(new AccountResource(accountDAO, accountLocationDAO));
        environment.addProvider(new RoomConditionsResource(deviceDataDAODynamoDB, deviceDAO, configuration.getAllowedQueryRange(),senseColorDAO, calibrationDAO));
        environment.addResource(new DeviceResources(deviceDAO, mergedUserInfoDynamoDB, sensorsViewsDynamoDB, pillHeartBeatDAODynamoDB));

        final S3BucketConfiguration provisionKeyConfiguration = configuration.getProvisionKeyConfiguration();

        final KeyStoreUtils keyStoreUtils = KeyStoreUtils.build(amazonS3, provisionKeyConfiguration.getBucket(), provisionKeyConfiguration.getKey());
        environment.addResource(new ProvisionResource(senseKeyStore, pillKeyStore, keyStoreUtils, pillProvisionDAO, amazonS3));

        final TimelineProcessor timelineProcessor = TimelineProcessor.createTimelineProcessor(
                pillDataDAODynamoDB,
                deviceDAO,
                deviceDataDAODynamoDB,
                ringTimeHistoryDAODynamoDB,
                feedbackDAO,
                sleepHmmDAODynamoDB,
                accountDAO,
                sleepStatsDAODynamoDB,
                senseColorDAO,
                onlineHmmModelsDAO,
                featureExtractionDAO,
                calibrationDAO,
                defaultModelEnsembleDAO,
                userTimelineTestGroupDAO);
                

        environment.addResource(new TimelineResource(accountDAO, timelineDAODynamoDB, timelineLogDAO,timelineLogger, timelineProcessor));

        environment.addResource(new TimeZoneResource(timeZoneHistoryDAODynamoDB, mergedUserInfoDynamoDB, deviceDAO));
        environment.addResource(new AlarmResource(alarmDAODynamoDB, mergedUserInfoDynamoDB, deviceDAO, amazonS3));

        environment.addResource(new MobilePushRegistrationResource(notificationSubscriptionDAOWrapper, mobilePushNotificationProcessor, accountDAO));

        final QuestionProcessor questionProcessor = new QuestionProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withCheckSkipsNum(configuration.getQuestionConfigs().getNumSkips())
                .withQuestions(questionResponseDAO)
                .build();
        environment.addResource(new QuestionsResource(accountDAO, timeZoneHistoryDAODynamoDB, questionProcessor));
        environment.addResource(new FeedbackResource(feedbackDAO, timelineDAODynamoDB));
        environment.addResource(new AppCheckinResource(2015000000));

        // data science resource stuff
        final AmazonDynamoDB prefsClient = dynamoDBClientFactory.getForEndpoint(configuration.getPreferencesDBConfiguration().getEndpoint());
        final AccountPreferencesDAO accountPreferencesDAO = AccountPreferencesDynamoDB.create(prefsClient, configuration.getPreferencesDBConfiguration().getTableName());
        environment.addResource(new AccountPreferencesResource(accountPreferencesDAO));

        environment.addResource(new InsightsResource(accountDAO, trendsInsightsDAO, insightsDAODynamoDB, sleepStatsDAODynamoDB));
        environment.addResource(new com.hello.suripu.app.v2.InsightsResource(insightsDAODynamoDB, trendsInsightsDAO));

        LOGGER.debug("{}", DateTime.now(DateTimeZone.UTC).getMillis());

        if(configuration.getDebug()) {
            environment.addResource(new VersionResource());
            environment.addResource(new PingResource());
        }

        final AmazonDynamoDB passwordResetDynamoDBClient = dynamoDBClientFactory.getForEndpoint(configuration.getPasswordResetDBConfiguration().getEndpoint());
        final PasswordResetDB passwordResetDB = PasswordResetDB.create(passwordResetDynamoDBClient, configuration.getPasswordResetDBConfiguration().getTableName());
        final DeviceProcessor deviceProcessor = new DeviceProcessor.Builder()
                .withDeviceDAO(deviceDAO)
                .withMergedUserInfoDynamoDB(mergedUserInfoDynamoDB)
                .withSensorsViewDynamoDB(sensorsViewsDynamoDB)
                .withPillDataDAODynamoDB(pillDataDAODynamoDB)
                .withWifiInfoDAO(wifiInfoDAO)
                .withSenseColorDAO(senseColorDAO)
                .withPillHeartbeatDAO(pillHeartBeatDAODynamoDB)
                .build();

        environment.addResource(PasswordResetResource.create(accountDAO, passwordResetDB, configuration.emailConfiguration()));

        environment.addResource(new SupportResource(supportDAO));
        environment.addResource(new com.hello.suripu.app.v2.TimelineResource(timelineDAODynamoDB, timelineProcessor, timelineLogDAO, feedbackDAO, pillDataDAODynamoDB, sleepStatsDAODynamoDB,timelineLogger));
        environment.addResource(new DeviceResource(deviceProcessor));
        environment.addResource(new com.hello.suripu.app.v2.AccountPreferencesResource(accountPreferencesDAO));
        StoreFeedbackDAO storeFeedbackDAO = commonDB.onDemand(StoreFeedbackDAO.class);
        environment.addResource(new StoreFeedbackResource(storeFeedbackDAO));
        environment.addResource(new AppStatsResource(appStatsDAO, insightsDAODynamoDB, questionProcessor, accountDAO, timeZoneHistoryDAODynamoDB));

        final TrendsProcessor trendsProcessor = new TrendsProcessor(sleepStatsDAODynamoDB, accountDAO, timeZoneHistoryDAODynamoDB);
        environment.addResource(new TrendsResource(trendsProcessor));
    }
}

package com.hello.suripu.app;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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
import com.hello.suripu.app.cli.RecreateEventsCommand;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.modules.RolloutAppModule;
import com.hello.suripu.app.resources.v1.AccountPreferencesResource;
import com.hello.suripu.app.resources.v1.AccountResource;
import com.hello.suripu.app.resources.v1.AlarmResource;
import com.hello.suripu.app.resources.v1.AppCheckinResource;
import com.hello.suripu.app.resources.v1.ApplicationResource;
import com.hello.suripu.app.resources.v1.DataScienceResource;
import com.hello.suripu.app.resources.v1.DeviceResources;
import com.hello.suripu.app.resources.v1.EventResource;
import com.hello.suripu.app.resources.v1.FeaturesResource;
import com.hello.suripu.app.resources.v1.FeedbackResource;
import com.hello.suripu.app.resources.v1.FirmwareResource;
import com.hello.suripu.app.resources.v1.InsightsResource;
import com.hello.suripu.app.resources.v1.MobilePushRegistrationResource;
import com.hello.suripu.app.resources.v1.OAuthResource;
import com.hello.suripu.app.resources.v1.QuestionsResource;
import com.hello.suripu.app.resources.v1.RoomConditionsResource;
import com.hello.suripu.app.resources.v1.ScoresResource;
import com.hello.suripu.app.resources.v1.SleepLabelResource;
import com.hello.suripu.app.resources.v1.TeamsResource;
import com.hello.suripu.app.resources.v1.TimeZoneResource;
import com.hello.suripu.app.resources.v1.TimelineResource;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.bundles.KinesisLoggerBundle;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.EventDAODynamoDB;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsDAO;
import com.hello.suripu.core.notifications.DynamoDBNotificationSubscriptionDAO;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;
import com.hello.suripu.core.notifications.NotificationSubscriptionsDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.firmware.FirmwareUpdateDAO;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.core.util.CustomJSONExceptionMapper;
import com.hello.suripu.core.util.DropwizardServiceUtil;
import com.hello.suripu.core.util.SunData;
import com.sun.jersey.api.core.ResourceConfig;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
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
        bootstrap.addCommand(new RecreateEventsCommand());
        bootstrap.addCommand(new CreateDynamoDBTables());
        bootstrap.addBundle(new KinesisLoggerBundle<SuripuAppConfiguration>() {
            @Override
            public KinesisLoggerConfiguration getConfiguration(final SuripuAppConfiguration configuration) {
                return configuration.getKinesisLoggerConfiguration();
            }
        });
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

        insightsDB.registerArgumentFactory(new JodaArgumentFactory());
        insightsDB.registerContainerFactory(new OptionalContainerFactory());
        insightsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = sensorsDB.onDemand(DeviceDAO.class);

        final SleepLabelDAO sleepLabelDAO = commonDB.onDemand(SleepLabelDAO.class);
        final SleepScoreDAO sleepScoreDAO = commonDB.onDemand(SleepScoreDAO.class);
        final TrendsDAO trendsDAO = insightsDB.onDemand(TrendsDAO.class);

        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);
        final QuestionResponseDAO questionResponseDAO = insightsDB.onDemand(QuestionResponseDAO.class);
        final FeedbackDAO feedbackDAO = commonDB.onDemand(FeedbackDAO.class);

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);
        final PersistentAccessTokenStore accessTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.withConnectionTimeout(200); // in ms
        clientConfiguration.withMaxErrorRetry(1);

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        dynamoDBClient.setEndpoint(configuration.getEventDBConfiguration().getEndpoint());

        final AmazonSNSClient snsClient = new AmazonSNSClient(awsCredentialsProvider, clientConfiguration);
        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider, clientConfiguration);

        final AmazonS3 amazonS3 = new AmazonS3Client(awsCredentialsProvider, clientConfiguration);

        final String eventTableName = configuration.getEventDBConfiguration().getTableName();

        final EventDAODynamoDB eventDAODynamoDB = new EventDAODynamoDB(dynamoDBClient, eventTableName);
        final AlarmDAODynamoDB alarmDAODynamoDB = new AlarmDAODynamoDB(
                dynamoDBClient, configuration.getAlarmDBConfiguration().getTableName()
        );

        final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(
                dynamoDBClient, configuration.getTimeZoneHistoryDBConfiguration().getTableName()
        );

        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(
                dynamoDBClient, configuration.getAlarmInfoDynamoDBConfiguration().getTableName()
        );

        final InsightsDAODynamoDB insightsDAODynamoDB = new InsightsDAODynamoDB(
                dynamoDBClient, configuration.getInsightsDynamoDBConfiguration().getTableName());

        // for localhost debug, may need to point to prod to get data
        final AmazonDynamoDBClient dynamoDBScoreClient = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);

        dynamoDBScoreClient.setEndpoint(configuration.getSleepScoreDBConfiguration().getEndpoint());
        final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                dynamoDBScoreClient,
                configuration.getSleepScoreDBConfiguration().getTableName(),
                configuration.getSleepScoreVersion()
        );

        final JedisPool jedisPool = new JedisPool("localhost", 6379);
        final ImmutableMap<String, String> arns = ImmutableMap.copyOf(configuration.getPushNotificationsConfiguration().getArns());
        final AmazonDynamoDB notificationsDynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        notificationsDynamoDBClient.setEndpoint(configuration.getNotificationsDBConfiguration().getEndpoint());

        final NotificationSubscriptionsDAO subscriptionDAO = new DynamoDBNotificationSubscriptionDAO(
                notificationsDynamoDBClient,
                configuration.getNotificationsDBConfiguration().getTableName(),
                snsClient,
                arns
        );

        final MobilePushNotificationProcessor mobilePushNotificationProcessor = new MobilePushNotificationProcessor(snsClient, subscriptionDAO);
        
        final TeamStore teamStore = new TeamStore(dynamoDBClient, "teams");

        final ImmutableMap<QueueName, String> streams = ImmutableMap.copyOf(configuration.getKinesisConfiguration().getStreams());
        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(kinesisClient, streams);
        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);

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

        LOGGER.warn("DEBUG MODE = {}", configuration.getDebug());
        // Custom JSON handling for responses.
        final ResourceConfig jrConfig = environment.getJerseyResourceConfig();
        DropwizardServiceUtil.deregisterDWSingletons(jrConfig);
        environment.addProvider(new CustomJSONExceptionMapper(configuration.getDebug()));

        environment.addProvider(new OAuthProvider(new OAuthAuthenticator(accessTokenStore), "protected-resources", activityLogger));

        final String namespace = (configuration.getDebug()) ? "dev" : "prod";
        dynamoDBClient.setEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final FeatureStore featureStore = new FeatureStore(dynamoDBClient, "features", namespace);


        final RolloutAppModule module = new RolloutAppModule(featureStore, 30);
        ObjectGraphRoot.getInstance().init(module);

        final KeyStore senseKeyStore = new KeyStoreDynamoDB(
                dynamoDBClient,
                configuration.getKeyStoreDynamoDBConfiguration().getTableName(),
                "1234567891234567".getBytes(), // TODO: REMOVE THIS WHEN WE ARE NOT SUPPOSED TO HAVE A DEFAULT KEY
                120 // 2 minutes for cache
        );

        environment.addResource(new OAuthResource(accessTokenStore, applicationStore, accountDAO, subscriptionDAO));
        environment.addResource(new AccountResource(accountDAO));
        environment.addResource(new ApplicationResource(applicationStore));
        environment.addResource(new SleepLabelResource(sleepLabelDAO));
        environment.addProvider(new RoomConditionsResource(accountDAO, deviceDataDAO, deviceDAO, configuration.getAllowedQueryRange()));
        environment.addResource(new EventResource(eventDAODynamoDB));
        environment.addResource(new DeviceResources(deviceDAO, accountDAO, mergedUserInfoDynamoDB, jedisPool, senseKeyStore));

        environment.addResource(new ScoresResource(trackerMotionDAO, sleepLabelDAO, sleepScoreDAO, aggregateSleepScoreDAODynamoDB, configuration.getScoreThreshold(), configuration.getSleepScoreVersion()));

        final SunData sunData = new SunData();
        environment.addResource(new TimelineResource(trackerMotionDAO, accountDAO, deviceDAO, deviceDataDAO, sleepLabelDAO, sleepScoreDAO, trendsDAO, aggregateSleepScoreDAODynamoDB, configuration.getScoreThreshold(), sunData, amazonS3, "hello-audio"));

        environment.addResource(new TimeZoneResource(timeZoneHistoryDAODynamoDB, mergedUserInfoDynamoDB, deviceDAO));
        environment.addResource(new AlarmResource(alarmDAODynamoDB, mergedUserInfoDynamoDB, deviceDAO, amazonS3));

        environment.addResource(new MobilePushRegistrationResource(subscriptionDAO, mobilePushNotificationProcessor, accountDAO));
        environment.addResource(new FeaturesResource(featureStore));

        environment.addResource(new QuestionsResource(accountDAO, questionResponseDAO, timeZoneHistoryDAODynamoDB, configuration.getQuestionConfigs().getNumSkips()));
        environment.addResource(new InsightsResource(accountDAO, trendsDAO, aggregateSleepScoreDAODynamoDB, trackerMotionDAO, insightsDAODynamoDB));
        environment.addResource(new TeamsResource(teamStore));
        environment.addResource(new FeedbackResource(feedbackDAO));
        environment.addResource(new AppCheckinResource(false, "")); // TODO: replace this with real app version. Maybe move it to admin tool?

        // data science resource stuff
        final AccountInfoProcessor.Builder builder = new AccountInfoProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withMapping(questionResponseDAO);
        final AccountInfoProcessor accountInfoProcessor = builder.build();

        final InsightProcessor.Builder insightBuilder = new InsightProcessor.Builder()
                .withSenseDAOs(deviceDataDAO, deviceDAO)
                .withTrackerMotionDAOs(trackerMotionDAO)
                .withInsightsDAOs(trendsDAO)
                .withDynamoDBDAOs(aggregateSleepScoreDAODynamoDB, insightsDAODynamoDB)
                .withAccountInfoProcessor(accountInfoProcessor)
                .withLightData(new LightData());
        final InsightProcessor insightProcessor = insightBuilder.build();

        environment.addResource(new DataScienceResource(accountDAO, trackerMotionDAO, deviceDataDAO, deviceDAO, insightProcessor));

        final AmazonDynamoDB prefsClient = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        prefsClient.setEndpoint(configuration.getPreferencesDBConfiguration().getEndpoint());
        final AccountPreferencesDAO accountPreferencesDAO = new AccountPreferencesDynamoDB(prefsClient, configuration.getPreferencesDBConfiguration().getTableName());
        environment.addResource(new AccountPreferencesResource(accountPreferencesDAO));

        final FirmwareUpdateDAO firmwareUpdateDAO = commonDB.onDemand(FirmwareUpdateDAO.class);
        final AmazonS3Client s3Client = new AmazonS3Client(awsCredentialsProvider);
        final FirmwareUpdateStore firmwareUpdateStore = new FirmwareUpdateStore(firmwareUpdateDAO, s3Client, "hello-firmware");
        environment.addResource(new FirmwareResource(firmwareUpdateStore, "hello-firmware", amazonS3)); // TODO: move logic from resource to FirmwareUpdateStore

        LOGGER.debug("{}", DateTime.now(DateTimeZone.UTC).getMillis());

        if(configuration.getDebug()) {
            environment.addResource(new VersionResource());
            environment.addResource(new PingResource());
        }
    }
}

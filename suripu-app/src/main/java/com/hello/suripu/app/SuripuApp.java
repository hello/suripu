package com.hello.suripu.app;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.app.cli.CreateAlarmDynamoDBTableCommand;
import com.hello.suripu.app.cli.CreateAlarmInfoDynamoDBTable;
import com.hello.suripu.app.cli.CreateDynamoDBEventTableCommand;
import com.hello.suripu.app.cli.CreateDynamoDBTimeZoneHistoryTableCommand;
import com.hello.suripu.app.cli.CreateRingTimeDynamoDBTable;
import com.hello.suripu.app.cli.CreateSleepScoreDynamoDBTable;
import com.hello.suripu.app.cli.RecreateEventsCommand;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.resources.v1.AccountResource;
import com.hello.suripu.app.resources.v1.AlarmResource;
import com.hello.suripu.app.resources.v1.ApplicationResource;
import com.hello.suripu.app.resources.v1.DeviceResources;
import com.hello.suripu.app.resources.v1.EventResource;
import com.hello.suripu.app.resources.v1.FirmwareResource;
import com.hello.suripu.app.resources.v1.InsightsResource;
import com.hello.suripu.app.resources.v1.MobilePushRegistrationResource;
import com.hello.suripu.app.resources.v1.OAuthResource;
import com.hello.suripu.app.resources.v1.QuestionsResource;
import com.hello.suripu.app.resources.v1.RoomConditionsResource;
import com.hello.suripu.app.resources.v1.ScoresResource;
import com.hello.suripu.app.resources.v1.SleepLabelResource;
import com.hello.suripu.app.resources.v1.TimeZoneResource;
import com.hello.suripu.app.resources.v1.TimelineResource;
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
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.notifications.DynamoDBNotificationSubscriptionDAO;
import com.hello.suripu.core.db.notifications.NotificationSubscriptionsDAO;
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
        bootstrap.addCommand(new CreateDynamoDBEventTableCommand());
        bootstrap.addCommand(new CreateDynamoDBTimeZoneHistoryTableCommand());
        bootstrap.addCommand(new CreateAlarmDynamoDBTableCommand());
        bootstrap.addCommand(new CreateRingTimeDynamoDBTable());
        bootstrap.addCommand(new CreateAlarmInfoDynamoDBTable());
        bootstrap.addCommand(new CreateSleepScoreDynamoDBTable());

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

        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());


        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = sensorsDB.onDemand(DeviceDAO.class);

        final SleepLabelDAO sleepLabelDAO = commonDB.onDemand(SleepLabelDAO.class);
        final SleepScoreDAO sleepScoreDAO = commonDB.onDemand(SleepScoreDAO.class);
        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);
        final PersistentAccessTokenStore accessTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);
        dynamoDBClient.setEndpoint(configuration.getEventDBConfiguration().getEndpoint());

        final AmazonSNSClient snsClient = new AmazonSNSClient(awsCredentialsProvider);
        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider);

        final AmazonS3 amazonS3 = new AmazonS3Client(awsCredentialsProvider);

        final String eventTableName = configuration.getEventDBConfiguration().getTableName();

        final EventDAODynamoDB eventDAODynamoDB = new EventDAODynamoDB(dynamoDBClient, eventTableName);
        final AlarmDAODynamoDB alarmDAODynamoDB = new AlarmDAODynamoDB(
                dynamoDBClient, configuration.getAlarmDBConfiguration().getTableName()
        );

        final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(
                dynamoDBClient, configuration.getTimeZoneHistoryDBConfiguration().getTableName()
        );

        final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB = new MergedAlarmInfoDynamoDB(
                dynamoDBClient, configuration.getAlarmInfoDynamoDBConfiguration().getTableName()
        );

        final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB = new AggregateSleepScoreDAODynamoDB(
                dynamoDBClient,
                configuration.getSleepScoreDBConfiguration().getTableName(),
                configuration.getSleepScoreVersion()
        );

        final ImmutableMap<String, String> arns = ImmutableMap.copyOf(configuration.getPushNotificationsConfiguration().getArns());
        final NotificationSubscriptionsDAO subscriptionDAO = new DynamoDBNotificationSubscriptionDAO(
                dynamoDBClient,
                configuration.getPushNotificationsConfiguration().getTableName(),
                snsClient,
                arns
        );

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

        environment.addResource(new OAuthResource(accessTokenStore, applicationStore, accountDAO, subscriptionDAO));
        environment.addResource(new AccountResource(accountDAO));
        environment.addResource(new ApplicationResource(applicationStore));
        environment.addResource(new SleepLabelResource(sleepLabelDAO));
        environment.addProvider(new RoomConditionsResource(deviceDataDAO, deviceDAO, configuration.getAllowedQueryRange()));
        environment.addResource(new EventResource(eventDAODynamoDB));
        environment.addResource(new DeviceResources(deviceDAO, accountDAO));

        environment.addResource(new ScoresResource(trackerMotionDAO, sleepLabelDAO, sleepScoreDAO, aggregateSleepScoreDAODynamoDB, configuration.getScoreThreshold(), configuration.getSleepScoreVersion()));

        final SunData sunData = new SunData();
        environment.addResource(new TimelineResource(trackerMotionDAO, deviceDAO, sleepLabelDAO, sleepScoreDAO, aggregateSleepScoreDAODynamoDB, configuration.getScoreThreshold(), sunData));

        environment.addResource(new TimeZoneResource(timeZoneHistoryDAODynamoDB, mergedAlarmInfoDynamoDB, deviceDAO));
        environment.addResource(new AlarmResource(alarmDAODynamoDB, mergedAlarmInfoDynamoDB, deviceDAO));

        environment.addResource(new MobilePushRegistrationResource(subscriptionDAO));

        environment.addResource(new QuestionsResource(accountDAO));
        environment.addResource(new InsightsResource(accountDAO));

        final FirmwareUpdateDAO firmwareUpdateDAO = commonDB.onDemand(FirmwareUpdateDAO.class);
        final AmazonS3Client s3Client = new AmazonS3Client(awsCredentialsProvider);
        final FirmwareUpdateStore firmwareUpdateStore = new FirmwareUpdateStore(firmwareUpdateDAO, s3Client);
        environment.addResource(new FirmwareResource(firmwareUpdateStore, "hello-firmware", amazonS3));

        LOGGER.debug("{}", DateTime.now(DateTimeZone.UTC).getMillis());

        if(configuration.getDebug()) {
            environment.addResource(new VersionResource());
            environment.addResource(new PingResource());
        }
    }
}

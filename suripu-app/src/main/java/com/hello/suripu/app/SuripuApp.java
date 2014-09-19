package com.hello.suripu.app;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.google.common.base.Joiner;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.app.cli.CreateDynamoDBEventTableCommand;
import com.hello.suripu.app.cli.CreateDynamoDBTimeZoneHistoryTableCommand;
import com.hello.suripu.app.cli.RecreateEventsCommand;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.resources.AccountResource;
import com.hello.suripu.app.resources.ApplicationResource;
import com.hello.suripu.app.resources.DeviceResources;
import com.hello.suripu.app.resources.EventResource;
import com.hello.suripu.app.resources.HistoryResource;
import com.hello.suripu.app.resources.OAuthResource;
import com.hello.suripu.app.resources.RoomConditionsResource;
import com.hello.suripu.app.resources.SleepLabelResource;
import com.hello.suripu.app.resources.TimelineResource;
import com.hello.suripu.app.resources.v1.MobilePushRegistrationResource;
import com.hello.suripu.app.resources.v1.QuestionsResource;
import com.hello.suripu.core.bundles.KinesisLoggerBundle;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.EventDAODynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.SoundDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.notifications.DynamoDBNotificationSubscriptionDAO;
import com.hello.suripu.core.db.notifications.NotificationSubscriptionsDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.core.util.CustomJSONExceptionMapper;
import com.hello.suripu.core.util.DropwizardServiceUtil;
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

        final SoundDAO soundDAO = sensorsDB.onDemand(SoundDAO.class);
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
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final AmazonSNSClient snsClient = new AmazonSNSClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getEventDBConfiguration().getEndpoint());
        final String eventTableName = configuration.getEventDBConfiguration().getTableName();
        final EventDAODynamoDB eventDAODynamoDB = new EventDAODynamoDB(client, eventTableName);

        final NotificationSubscriptionsDAO subscriptionDAO = new DynamoDBNotificationSubscriptionDAO(client, "notifications");



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

        environment.addProvider(new OAuthProvider<AccessToken>(new OAuthAuthenticator(accessTokenStore), "protected-resources"));

        environment.addResource(new OAuthResource(accessTokenStore, applicationStore, accountDAO));
        environment.addResource(new AccountResource(accountDAO));
        environment.addResource(new HistoryResource(soundDAO, trackerMotionDAO, deviceDAO, deviceDataDAO));
        environment.addResource(new ApplicationResource(applicationStore));
        environment.addResource(new SleepLabelResource(sleepLabelDAO));
        environment.addProvider(new RoomConditionsResource(deviceDataDAO, deviceDAO, configuration.getAllowedQueryRange()));
        environment.addResource(new EventResource(eventDAODynamoDB));
        environment.addResource(new DeviceResources(deviceDAO));
        environment.addResource(new TimelineResource(eventDAODynamoDB, trackerMotionDAO, sleepLabelDAO, sleepScoreDAO, configuration.getScoreThreshold()));

//        final InMemoryNotificationSubscriptionDAO subscriptionDAO = new InMemoryNotificationSubscriptionDAO();
        environment.addResource(new MobilePushRegistrationResource(snsClient, subscriptionDAO));


        environment.addResource(new com.hello.suripu.app.resources.v1.OAuthResource(accessTokenStore, applicationStore, accountDAO));
        environment.addResource(new com.hello.suripu.app.resources.v1.AccountResource(accountDAO));
        environment.addResource(new com.hello.suripu.app.resources.v1.HistoryResource(soundDAO, trackerMotionDAO, deviceDAO, deviceDataDAO));
        environment.addResource(new com.hello.suripu.app.resources.v1.ApplicationResource(applicationStore));
        environment.addResource(new com.hello.suripu.app.resources.v1.SleepLabelResource(sleepLabelDAO));
        environment.addProvider(new com.hello.suripu.app.resources.v1.RoomConditionsResource(deviceDataDAO, deviceDAO, configuration.getAllowedQueryRange()));
        environment.addResource(new com.hello.suripu.app.resources.v1.EventResource(eventDAODynamoDB));
        environment.addResource(new com.hello.suripu.app.resources.v1.DeviceResources(deviceDAO));
        environment.addResource(new com.hello.suripu.app.resources.v1.TimelineResource(eventDAODynamoDB, trackerMotionDAO, sleepLabelDAO, sleepScoreDAO, configuration.getScoreThreshold()));

        environment.addResource(new QuestionsResource(accountDAO));


        LOGGER.debug("{}", DateTime.now(DateTimeZone.UTC).getMillis());

        if(configuration.getDebug()) {
            environment.addResource(new VersionResource());
            environment.addResource(new PingResource());
        }
    }
}

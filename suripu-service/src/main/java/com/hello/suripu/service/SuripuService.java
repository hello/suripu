package com.hello.suripu.service;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.base.Joiner;
import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.EventDAO;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.PublicKeyStore;
import com.hello.suripu.core.db.PublicKeyStoreDynamoDB;
import com.hello.suripu.core.db.ScoreDAO;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.firmware.FirmwareUpdateDAO;
import com.hello.suripu.core.firmware.FirmwareUpdateStore;
import com.hello.suripu.core.flipper.GroupFlipper;
import com.hello.suripu.core.health.DynamoDbHealthCheck;
import com.hello.suripu.core.health.KinesisHealthCheck;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.managers.DynamoDBClientManaged;
import com.hello.suripu.core.managers.KinesisClientManaged;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.service.cli.CreateKeyStoreDynamoDBTable;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.hello.suripu.service.modules.RolloutModule;
import com.hello.suripu.service.resources.AudioResource;
import com.hello.suripu.service.resources.DownloadResource;
import com.hello.suripu.service.resources.LogsResource;
import com.hello.suripu.service.resources.ReceiveResource;
import com.hello.suripu.service.resources.RegisterResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.GraphiteReporter;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SuripuService extends Service<SuripuConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SuripuService.class);

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new SuripuService().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addCommand(new CreateKeyStoreDynamoDBTable());
    }

    @Override
    public void run(SuripuConfiguration configuration, Environment environment) throws Exception {
        environment.addProvider(new JacksonProtobufProvider());

        final DBIFactory factory = new DBIFactory();
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql");
        final DBI sensorsDB = factory.build(environment, configuration.getSensorsDB(), "postgresql");

        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());


        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = sensorsDB.onDemand(DeviceDAO.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final ScoreDAO scoreDAO = commonDB.onDemand(ScoreDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);
        final FirmwareUpdateDAO firmwareUpdateDAO = commonDB.onDemand(FirmwareUpdateDAO.class);

        final EventDAO eventDAO = commonDB.onDemand(EventDAO.class);

        // Checks Environment first and then instance profile.
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);
        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        final AmazonS3Client s3Client = new AmazonS3Client(awsCredentialsProvider);
        final String bucketName = configuration.getAudioBucketName();

        final AlarmDAODynamoDB alarmDAODynamoDB = new AlarmDAODynamoDB(dynamoDBClient, configuration.getAlarmDBConfiguration().getTableName());
        final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB = new MergedAlarmInfoDynamoDB(dynamoDBClient,
                configuration.getAlarmInfoDynamoDBConfiguration().getTableName());
        final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(dynamoDBClient, configuration.getTimeZoneHistoryDBConfiguration().getTableName());



        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider);
        kinesisClient.setEndpoint(configuration.getKinesisConfiguration().getEndpoint());

        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(
                kinesisClient,
                configuration.getKinesisConfiguration().getStreams()
        );

        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        // TODO; set region here?

        final PublicKeyStore publicKeyStore = new PublicKeyStoreDynamoDB(
                dynamoDBClient,
                configuration.getDynamoDBConfiguration().getTableName()
        );

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

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

        final FirmwareUpdateStore firmwareUpdateStore = new FirmwareUpdateStore(firmwareUpdateDAO, s3Client);

        final DataLogger activityLogger = kinesisLoggerFactory.get(QueueName.ACTIVITY_STREAM);
        environment.addProvider(new OAuthProvider(new OAuthAuthenticator(tokenStore), "protected-resources", activityLogger));
        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        final TeamStore teamStore = new TeamStore(dynamoDBClient, "teams");
        final GroupFlipper groupFlipper = new GroupFlipper(teamStore, 10);

        final FeatureStore featureStore = new FeatureStore(dynamoDBClient, "features", "namespace1");
        final RolloutModule module = new RolloutModule(featureStore, 10);
        ObjectGraphRoot.getInstance().init(module);

        final ReceiveResource receiveResource = new ReceiveResource(deviceDataDAO, deviceDAO,
                publicKeyStore,
                kinesisLoggerFactory,
                mergedAlarmInfoDynamoDB,
                configuration.getDebug(),
                configuration.getRoomConditions(),
                firmwareUpdateStore,
                groupFlipper
        );



        environment.addResource(receiveResource);
        environment.addResource(new RegisterResource(deviceDAO, tokenStore, kinesisLoggerFactory, configuration.getDebug()));
        environment.addResource(new LogsResource(
                configuration.getIndexLogConfiguration().getPrivateUrl(),
                configuration.getIndexLogConfiguration().getIndexName())
        );

        environment.addResource(new PingResource());
        environment.addResource(new VersionResource());

        final DataLogger audioDataLogger = kinesisLoggerFactory.get(QueueName.AUDIO_FEATURES);
        environment.addResource(new AudioResource(s3Client, bucketName, audioDataLogger, configuration.getDebug()));

        environment.addResource(new DownloadResource(s3Client, "hello-firmware"));

        // Manage the lifecycle of our clients
        environment.manage(new DynamoDBClientManaged(dynamoDBClient));
        environment.manage(new KinesisClientManaged(kinesisClient));

        // Make sure we can connect
        environment.addHealthCheck(new DynamoDbHealthCheck(dynamoDBClient));
        environment.addHealthCheck(new KinesisHealthCheck(kinesisClient));
    }


}

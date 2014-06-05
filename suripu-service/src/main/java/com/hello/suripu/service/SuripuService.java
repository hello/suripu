package com.hello.suripu.service;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.EventDAO;
import com.hello.suripu.core.db.PublicKeyStore;
import com.hello.suripu.core.db.PublicKeyStoreDynamoDB;
import com.hello.suripu.core.db.ScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrackerMotionDAODynamoDB;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.health.DynamoDbHealthCheck;
import com.hello.suripu.core.health.KinesisHealthCheck;
import com.hello.suripu.core.managers.DynamoDBClientManaged;
import com.hello.suripu.core.managers.KinesisClientManaged;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.models.KinesisLogger;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentAccessTokenStore;
import com.hello.suripu.core.oauth.stores.PersistentApplicationStore;
import com.hello.suripu.service.cli.CreateDynamoDBTrackerTableCommand;
import com.hello.suripu.service.cli.MigrateTrackerDataCommand;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.hello.suripu.service.db.DeviceDataDAO;
import com.hello.suripu.service.resources.ReceiveResource;
import com.librato.metrics.LibratoReporter;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import com.yammer.metrics.core.MetricPredicate;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class SuripuService extends Service<SuripuConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SuripuService.class);

    public static void main(String[] args) throws Exception {
        new SuripuService().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addCommand(new CreateDynamoDBTrackerTableCommand());
        bootstrap.addCommand(new MigrateTrackerDataCommand());
        //bootstrap.addCommand(new DropTrackerDataTableCommand());
    }

    @Override
    public void run(SuripuConfiguration configuration, Environment environment) throws Exception {
        environment.addProvider(new JacksonProtobufProvider());

        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getDatabaseConfiguration(), "postgresql");
        jdbi.registerArgumentFactory(new JodaArgumentFactory());

        final DeviceDataDAO dao = jdbi.onDemand(DeviceDataDAO.class);
        final AccessTokenDAO accessTokenDAO = jdbi.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = jdbi.onDemand(DeviceDAO.class);
        final ApplicationsDAO applicationsDAO = jdbi.onDemand(ApplicationsDAO.class);
        final ScoreDAO scoreDAO = jdbi.onDemand(ScoreDAO.class);
        final TrackerMotionDAO trackerMotionDAO = jdbi.onDemand(TrackerMotionDAO.class);


        final EventDAO eventDAO = jdbi.onDemand(EventDAO.class);

        // Checks Environment first and then instance profile.
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);

        final String motionDataTableName = configuration.getMotionDBConfiguration().getKeyStoreTable();
        final TrackerMotionDAODynamoDB trackerMotionDAODynamoDB = new TrackerMotionDAODynamoDB(dynamoDBClient, motionDataTableName);


        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider);
        kinesisClient.setEndpoint(configuration.getKinesisConfiguration().getEndpoint());

        final KinesisLogger kinesisLogger = new KinesisLogger(kinesisClient, configuration.getKinesisConfiguration().getStreamName());

        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        // TODO; set region here?

        final PublicKeyStore publicKeyStore = new PublicKeyStoreDynamoDB(
                dynamoDBClient,
                configuration.getDynamoDBConfiguration().getKeyStoreTable()
        );

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        if(configuration.getMetricsEnabled()) {
            final String libratoUsername = configuration.getLibrato().getUsername();
            final String libratoApiKey = configuration.getLibrato().getApiKey();

            final MetricPredicate predicate = new RegexMetricPredicate("(^com\\.hello\\..*|^org\\.eclipse\\.jetty\\.servlet)");

            final InetAddress addr = InetAddress.getLocalHost();
//            final String hostname = addr.getHostName();
            final String hostname = "suripu-service"; // Consolidate all sources to come from a "single app". Cheaper :)

            LibratoReporter.enable(
                    LibratoReporter.builder(libratoUsername, libratoApiKey, hostname)
                            .setExpansionConfig(
                                    new LibratoReporter.MetricExpansionConfig(
                                            EnumSet.of(
                                                    LibratoReporter.ExpandedMetric.COUNT,
                                                    LibratoReporter.ExpandedMetric.MEDIAN,
                                                    LibratoReporter.ExpandedMetric.PCT_95,
                                                    LibratoReporter.ExpandedMetric.PCT_99,
                                                    LibratoReporter.ExpandedMetric.RATE_1_MINUTE
                                            )
                                    )
                            ).setPredicate(predicate)
                    ,
                    configuration.getLibrato().getReportingIntervalInSeconds(),
                    TimeUnit.SECONDS
            );
            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        environment.addProvider(new OAuthProvider<AccessToken>(new OAuthAuthenticator(tokenStore), "protected-resources"));

        environment.addResource(new ReceiveResource(dao, deviceDAO, scoreDAO,
                trackerMotionDAO,
                trackerMotionDAODynamoDB,
                publicKeyStore, kinesisLogger));
        environment.addResource(new PingResource());
        environment.addResource(new VersionResource());

        // Manage the lifecycle of our clients
        environment.manage(new DynamoDBClientManaged(dynamoDBClient));
        environment.manage(new KinesisClientManaged(kinesisClient));

        // Make sure we can connect
        environment.addHealthCheck(new DynamoDbHealthCheck(dynamoDBClient));
        environment.addHealthCheck(new KinesisHealthCheck(kinesisClient));
    }


}

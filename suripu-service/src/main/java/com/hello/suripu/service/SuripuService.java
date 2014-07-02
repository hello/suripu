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
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.health.DynamoDbHealthCheck;
import com.hello.suripu.core.health.KinesisHealthCheck;
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
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.service.resources.ReceiveResource;
import com.librato.metrics.LibratoReporter;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
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

        final DeviceDataDAO dao = sensorsDB.onDemand(DeviceDataDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = sensorsDB.onDemand(DeviceDAO.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final ScoreDAO scoreDAO = commonDB.onDemand(ScoreDAO.class);
        final TrackerMotionDAO trackerMotionDAO = sensorsDB.onDemand(TrackerMotionDAO.class);


        final EventDAO eventDAO = commonDB.onDemand(EventDAO.class);

        // Checks Environment first and then instance profile.
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        final AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(awsCredentialsProvider);


        final AmazonKinesisAsyncClient kinesisClient = new AmazonKinesisAsyncClient(awsCredentialsProvider);
        kinesisClient.setEndpoint(configuration.getKinesisConfiguration().getEndpoint());

        final KinesisLoggerFactory kinesisLoggerFactory = new KinesisLoggerFactory(kinesisClient, configuration.getKinesisConfiguration().getStreams());

        dynamoDBClient.setEndpoint(configuration.getDynamoDBConfiguration().getEndpoint());
        // TODO; set region here?

        final PublicKeyStore publicKeyStore = new PublicKeyStoreDynamoDB(
                dynamoDBClient,
                configuration.getDynamoDBConfiguration().getTableName()
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
                publicKeyStore, kinesisLoggerFactory));
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

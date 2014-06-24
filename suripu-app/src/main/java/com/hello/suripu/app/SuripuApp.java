package com.hello.suripu.app;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.hello.suripu.app.cli.CreateDynamoDBEventTableCommand;
import com.hello.suripu.app.cli.CreateDynamoDBTimeZoneHistoryTableCommand;
import com.hello.suripu.app.cli.RecreateEventsCommand;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.resources.AccountResource;
import com.hello.suripu.app.resources.ApplicationResource;
import com.hello.suripu.app.resources.EventResource;
import com.hello.suripu.app.resources.HistoryResource;
import com.hello.suripu.app.resources.OAuthResource;
import com.hello.suripu.app.resources.RoomConditionsResource;
import com.hello.suripu.app.resources.ScoreResource;
import com.hello.suripu.app.resources.SleepLabelResource;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.EventDAODynamoDB;
import com.hello.suripu.core.db.ScoreDAO;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.TimeSerieDAO;
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
import com.librato.metrics.LibratoReporter;
import com.sun.jersey.api.core.ResourceConfig;
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

public class SuripuApp extends Service<SuripuAppConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuripuApp.class);

    public static void main(final String[] args) throws Exception {
        new SuripuApp().run(args);
    }

    @Override
    public void initialize(final Bootstrap<SuripuAppConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addCommand(new RecreateEventsCommand());
        bootstrap.addCommand(new CreateDynamoDBEventTableCommand());
        bootstrap.addCommand(new CreateDynamoDBTimeZoneHistoryTableCommand());
    }

    @Override
    public void run(final SuripuAppConfiguration config, final Environment environment) throws Exception {



        final DBIFactory factory = new DBIFactory();
        final DBI sensorsDB = factory.build(environment, config.getSensorsDB(), "postgresql");
        final DBI commonDB = factory.build(environment, config.getCommonDB(), "postgresql");

        sensorsDB.registerArgumentFactory(new JodaArgumentFactory());
        sensorsDB.registerContainerFactory(new OptionalContainerFactory());
        sensorsDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());


        commonDB.registerArgumentFactory(new JodaArgumentFactory());
        commonDB.registerContainerFactory(new OptionalContainerFactory());
        commonDB.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        final TimeSerieDAO timeSerieDAO = sensorsDB.onDemand(TimeSerieDAO.class);
        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);
        final ApplicationsDAO applicationsDAO = commonDB.onDemand(ApplicationsDAO.class);
        final AccessTokenDAO accessTokenDAO = commonDB.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = sensorsDB.onDemand(DeviceDAO.class);
        final ScoreDAO scoreDAO = commonDB.onDemand(ScoreDAO.class);
        final SleepLabelDAO sleepLabelDAO = commonDB.onDemand(SleepLabelDAO.class);
        final DeviceDataDAO deviceDataDAO = sensorsDB.onDemand(DeviceDataDAO.class);

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);
        final PersistentAccessTokenStore accessTokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.getEventDBConfiguration().getEndpoint());
        final String eventTableName = config.getEventDBConfiguration().getTableName();
        final EventDAODynamoDB eventDAODynamoDB = new EventDAODynamoDB(client, eventTableName);



        if(config.getMetricsEnabled()) {
            final String libratoUsername = config.getLibrato().getUsername();
            final String libratoApiKey = config.getLibrato().getApiKey();

            final MetricPredicate predicate = new RegexMetricPredicate("(^com\\.hello\\..*|^org\\.eclipse\\.jetty\\.servlet)");

            final InetAddress addr = InetAddress.getLocalHost();
//            final String hostname = addr.getHostName();
            final String hostname = "suripu-app"; // Consolidate all sources to come from a "single app". Cheaper :)

            LibratoReporter.enable(
                    LibratoReporter.builder(libratoUsername, libratoApiKey, hostname)
                            .setExpansionConfig(
                                    new LibratoReporter.MetricExpansionConfig(
                                            EnumSet.of(
                                                    LibratoReporter.ExpandedMetric.COUNT,
                                                    LibratoReporter.ExpandedMetric.MEDIAN,
                                                    LibratoReporter.ExpandedMetric.PCT_95,
                                                    LibratoReporter.ExpandedMetric.PCT_99,
                                                    LibratoReporter.ExpandedMetric.RATE_1_MINUTE)
                                    )
                            ).setPredicate(predicate)
                    ,
                    config.getLibrato().getReportingIntervalInSeconds(),
                    TimeUnit.SECONDS
            );
            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

        environment.addProvider(new OAuthProvider<AccessToken>(new OAuthAuthenticator(accessTokenStore), "protected-resources"));



        // Custom JSON handling for responses.
        final ResourceConfig jrConfig = environment.getJerseyResourceConfig();
        DropwizardServiceUtil.deregisterDWSingletons(jrConfig);
        environment.addProvider(new CustomJSONExceptionMapper(Boolean.TRUE));


        environment.addResource(new OAuthResource(accessTokenStore, applicationStore, accountDAO));
        environment.addResource(new AccountResource(accountDAO));
        environment.addResource(new HistoryResource(timeSerieDAO, deviceDAO));
        environment.addResource(new ApplicationResource(applicationStore));
        environment.addResource(new ScoreResource(timeSerieDAO, deviceDAO, scoreDAO, accountDAO));
        environment.addResource(new SleepLabelResource(sleepLabelDAO));
        environment.addProvider(new RoomConditionsResource(deviceDataDAO));
        environment.addResource(new EventResource(eventDAODynamoDB));

//        environment.addHealthCheck(new DBIHealthCheck(sensorsDB, "account-db", "SELECT * FROM accounts ORDER BY ID DESC LIMIT 1;"));
    }
}

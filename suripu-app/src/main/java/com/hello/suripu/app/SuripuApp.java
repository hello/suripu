package com.hello.suripu.app;

import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.resources.ApplicationResource;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.db.TimeSerieDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.app.resources.AccountResource;
import com.hello.suripu.app.resources.HistoryResource;
import com.hello.suripu.app.resources.OAuthResource;


import com.hello.suripu.core.metrics.RegexMetricPredicate;

import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.OAuthTokenStore;
import com.hello.suripu.core.oauth.OAuthProvider;
import com.hello.suripu.core.oauth.OAuthAuthenticator;
import com.hello.suripu.core.oauth.InMemoryOAuthTokenStore;
import com.hello.suripu.core.oauth.PersistentApplicationStore;
import com.hello.suripu.service.db.JodaArgumentFactory;
import com.librato.metrics.LibratoReporter;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.DBIHealthCheck;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricsRegistry;
import org.skife.jdbi.v2.DBI;

import java.net.InetAddress;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class SuripuApp extends Service<SuripuAppConfiguration> {
    public static void main(String[] args) throws Exception {
        new SuripuApp().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuAppConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
    }

    @Override
    public void run(SuripuAppConfiguration config, Environment environment) throws Exception {

        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, config.getDatabaseConfiguration(), "postgresql");
        jdbi.registerArgumentFactory(new JodaArgumentFactory());
        jdbi.registerContainerFactory(new OptionalContainerFactory());
        jdbi.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());


        final TimeSerieDAO timeSerieDAO = jdbi.onDemand(TimeSerieDAO.class);
        final AccountDAO accountDAO = jdbi.onDemand(AccountDAOImpl.class);
        final ApplicationsDAO applicationsDAO = jdbi.onDemand(ApplicationsDAO.class);
        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        // TODO : remove everything below once we have persistent data stores.
        final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore = new InMemoryOAuthTokenStore();
        // TODO : remove everything above once we have persistent data stores.

        // TODO : move this in the configuration file
        final String libratoUsername= "tim@sayhello.com";
        final String libratoApiKey = "64ab12d5c69fa2b6873b7ace11aba06f4d8c93fa9b0cd7012a0cd2ba2f9c53ac";

        final MetricsRegistry registry = Metrics.defaultRegistry();


        final MetricPredicate predicate = new RegexMetricPredicate("(^com\\.hello\\..*|^org\\.eclipse\\.jetty\\.servlet)");

        final InetAddress addr = InetAddress.getLocalHost();
        final String hostname = addr.getHostName();

        LibratoReporter.enable(
                LibratoReporter.builder(libratoUsername, libratoApiKey, hostname)
                        .setExpansionConfig(
                                new LibratoReporter.MetricExpansionConfig(
                                        EnumSet.of(
                                                LibratoReporter.ExpandedMetric.COUNT,
                                                LibratoReporter.ExpandedMetric.MEDIAN,
                                                LibratoReporter.ExpandedMetric.PCT_95,
                                                LibratoReporter.ExpandedMetric.RATE_1_MINUTE)
                                )
                        ).setPredicate(predicate)
                ,
                30,
                TimeUnit.SECONDS
        );

        environment.addProvider(new OAuthProvider<ClientDetails>(new OAuthAuthenticator(tokenStore), "protected-resources"));

        environment.addResource(new OAuthResource(tokenStore, applicationStore, accountDAO));
        environment.addResource(new AccountResource(accountDAO, tokenStore));
        environment.addResource(new HistoryResource(timeSerieDAO));
        environment.addResource(new ApplicationResource(applicationStore));
        environment.addHealthCheck(new DBIHealthCheck(jdbi, "account-db", "SELECT * FROM accounts ORDER BY ID DESC LIMIT 1;"));
    }
}

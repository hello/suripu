package com.hello.suripu.app;

import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.resources.ApplicationResource;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.app.resources.AccountResource;
import com.hello.suripu.app.resources.HistoryResource;
import com.hello.suripu.app.resources.OAuthResource;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.TimeSerieDAO;
import com.hello.suripu.core.metrics.RegexMetricPredicate;
import com.hello.suripu.core.oauth.*;
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

        final TimeSerieDAO timeSerieDAO = jdbi.onDemand(TimeSerieDAO.class);
        final AccountDAO accountDAO = jdbi.onDemand(AccountDAOImpl.class);

        // TODO : remove everything below once we have persistent data stores.
        final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore = new InMemoryOAuthTokenStore();


//        final Registration registration = new Registration(
//                "pang",
//                "wu",
//                String.format("pang+%s@sayhello.com", next),
//                "my secret password",
//                Gender.OTHER,
//                200.0f,
//                99.0f,
//                99,
//                "America/Los_Angeles"
//        );
//
//        final Registration securedRegistration = Registration.encryptPassword(registration);
//        final Account account = accountDAO.register(securedRegistration);
//
//        final OAuthScope[] scopes = new OAuthScope[]{
//                OAuthScope.USER_BASIC,
//                OAuthScope.USER_EXTENDED,
//                OAuthScope.SENSORS_BASIC,
//                OAuthScope.SENSORS_EXTENDED,
//        };
//
//        final Application helloOAuthApplication = new Application(
//                1L,
//                "Hello OAuth Application",
//                "123456ClientId",
//                "654321ClientSecret",
//                "http://hello.com/oauth",
//                scopes,
//                666L,
//                "Official Hello Application",
//                Boolean.FALSE
//        );
//
        final InMemoryApplicationStore applicationStore = new InMemoryApplicationStore();
//        applicationStore.storeApplication(helloOAuthApplication);
//        applicationStore.activateForAccountId(helloOAuthApplication, 52L);

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

package com.hello.suripu.app;

import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.resources.AccountResource;
import com.hello.suripu.app.resources.HistoryResource;
import com.hello.suripu.app.resources.OAuthResource;
import com.hello.suripu.core.Account;
import com.hello.suripu.core.Gender;
import com.hello.suripu.core.Registration;
import com.hello.suripu.core.db.InMemoryAccountDAOImpl;
import com.hello.suripu.core.db.TimeSerieDAO;
import com.hello.suripu.core.oauth.*;
import com.hello.suripu.service.db.JodaArgumentFactory;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import org.skife.jdbi.v2.DBI;

public class SuripuApp extends Service<SuripuAppConfiguration> {
    public static void main(String[] args) throws Exception {
        new SuripuApp().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuAppConfiguration> bootstrap) {

    }

    @Override
    public void run(SuripuAppConfiguration config, Environment environment) throws Exception {


        final OAuthTokenStore<AccessToken,ClientDetails, ClientCredentials> tokenStore = new InMemoryOAuthTokenStore();
        final InMemoryAccountDAOImpl accountDAO = new InMemoryAccountDAOImpl();

        // TODO : remove everything below once we have persistent data stores.
        final Registration registration = new Registration(
                "tim",
                "bart",
                "tim@sayhello.com",
                "my secret password",
                Gender.OTHER,
                200.0f,
                99.0f,
                99,
                "America/Los_Angeles"
        );

        final Account account = accountDAO.register(registration);

        final OAuthScope[] scopes = new OAuthScope[]{
                OAuthScope.SENSORS_EXTENDED,
                OAuthScope.USER_BASIC,
                OAuthScope.USER_EXTENDED,
                OAuthScope.SENSORS_BASIC
        };

        final ClientDetails clientDetails = new ClientDetails(
                "responseType",
                "123456ClientId",
                "http://hello.com/oauth",
                scopes,
                "state",
                "code",
                1L,
                "654321ClientSecret"
        );

        final Application helloOAuthApplication = new Application(
                1L,
                "Hello OAuth Application",
                "123456ClientId",
                "654321ClientSecret",
                "http://hello.com/oauth",
                scopes,
                666L,
                "Official Hello Application",
                Boolean.FALSE
        );

        tokenStore.storeAccessToken(clientDetails);
        tokenStore.storeAuthorizationCode(clientDetails);


        final InMemoryApplicationStore applicationStore = new InMemoryApplicationStore();
        applicationStore.storeApplication(helloOAuthApplication);
        applicationStore.activateForAccountId(helloOAuthApplication, account.id);

        // TODO : remove everything above once we have persistent data stores.

        final DBIFactory factory = new DBIFactory();

        final DBI jdbi = factory.build(environment, config.getDatabaseConfiguration(), "postgresql");
        jdbi.registerArgumentFactory(new JodaArgumentFactory());
        final TimeSerieDAO timeSerieDAO = jdbi.onDemand(TimeSerieDAO.class);

        environment.addProvider(new OAuthProvider<ClientDetails>(new OAuthAuthenticator(tokenStore), "protected-resources"));

        environment.addResource(new OAuthResource(tokenStore, applicationStore, accountDAO));
        environment.addResource(new AccountResource(accountDAO, tokenStore));
        environment.addResource(new HistoryResource(timeSerieDAO));
    }
}

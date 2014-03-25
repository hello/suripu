package com.hello.suripu.app;

import com.google.common.base.Optional;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.app.resources.AccountResource;
import com.hello.suripu.app.resources.HistoryResource;
import com.hello.suripu.app.resources.OAuthResource;
import com.hello.suripu.core.Account;
import com.hello.suripu.core.db.AccountDAOImpl;
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

        final AccountDAOImpl accountDAO = new AccountDAOImpl();
        final Optional<Account> accountOptional = accountDAO.getById(1L);

        // Temporary, don't have a register page
        final Account account = accountOptional.get();
        final OAuthScope[] scopes = new OAuthScope[2];
        scopes[0] = OAuthScope.USER_BASIC;
        scopes[1] = OAuthScope.USER_EXTENDED;

        final ClientDetails clientDetails = new ClientDetails(
                "responseType",
                "clientId",
                "redirectUri",
                scopes,
                "state",
                "code",
                1L,
                "secret"
        );

        tokenStore.storeAccessToken(clientDetails);
        tokenStore.storeAuthorizationCode(clientDetails);


        final DBIFactory factory = new DBIFactory();

        final DBI jdbi = factory.build(environment, config.getDatabaseConfiguration(), "postgresql");
        jdbi.registerArgumentFactory(new JodaArgumentFactory());
        final TimeSerieDAO timeSerieDAO = jdbi.onDemand(TimeSerieDAO.class);

        environment.addProvider(new OAuthProvider<ClientDetails>(new OAuthAuthenticator(tokenStore), "protected-resources"));

        environment.addResource(new OAuthResource(tokenStore));
        environment.addResource(new AccountResource(accountDAO));
        environment.addResource(new HistoryResource(timeSerieDAO));
    }
}

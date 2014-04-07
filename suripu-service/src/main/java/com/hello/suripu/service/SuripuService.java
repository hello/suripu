package com.hello.suripu.service;

import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.core.db.AccessTokenDAO;
import com.hello.suripu.core.db.ApplicationsDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.oauth.*;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.hello.suripu.service.db.EventDAO;
import com.hello.suripu.service.db.JodaArgumentFactory;
import com.hello.suripu.service.resources.ReceiveResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import org.skife.jdbi.v2.DBI;

public class SuripuService extends Service<SuripuConfiguration> {

    public static void main(String[] args) throws Exception {
        new SuripuService().run(args);
    }

    @Override
    public void initialize(Bootstrap<SuripuConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
    }

    @Override
    public void run(SuripuConfiguration config, Environment environment) throws Exception {
        environment.addProvider(new JacksonProtobufProvider());

        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, config.getDatabaseConfiguration(), "postgresql");
        jdbi.registerArgumentFactory(new JodaArgumentFactory());

        final EventDAO dao = jdbi.onDemand(EventDAO.class);
        final AccessTokenDAO accessTokenDAO = jdbi.onDemand(AccessTokenDAO.class);
        final DeviceDAO deviceDAO = jdbi.onDemand(DeviceDAO.class);
        final ApplicationsDAO applicationsDAO = jdbi.onDemand(ApplicationsDAO.class);

        final PersistentApplicationStore applicationStore = new PersistentApplicationStore(applicationsDAO);

        final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore = new PersistentAccessTokenStore(accessTokenDAO, applicationStore);

        environment.addProvider(new OAuthProvider<AccessToken>(new OAuthAuthenticator(tokenStore), "protected-resources"));

        environment.addResource(new ReceiveResource(dao, deviceDAO));
        environment.addResource(new PingResource());
        environment.addResource(new VersionResource());
    }
}

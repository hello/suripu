package com.hello.suripu.service;

import com.hello.dropwizard.mikkusu.helpers.JacksonProtobufProvider;
import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.dropwizard.mikkusu.resources.VersionResource;
import com.hello.suripu.service.configuration.SuripuConfiguration;
import com.hello.suripu.service.db.EventDAO;
import com.hello.suripu.service.db.JodaArgumentFactory;
import com.hello.suripu.service.resources.ReceiveResource;
import com.sun.tools.jdi.resources.jdi;
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

        environment.addResource(new ReceiveResource(dao));

        environment.addResource(new PingResource());
        environment.addResource(new VersionResource());


    }
}

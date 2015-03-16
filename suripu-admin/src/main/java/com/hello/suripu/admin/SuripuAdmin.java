package com.hello.suripu.admin;

import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.hello.suripu.admin.resources.v1.AccountResource;
import com.hello.suripu.core.bundles.KinesisLoggerBundle;
import com.hello.suripu.core.configuration.KinesisLoggerConfiguration;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import com.yammer.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;

public class SuripuAdmin extends Service<SuripuAdminConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SuripuAdmin.class);

    public static void main(final String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        new SuripuAdmin().run(args);
    }

    @Override
    public void initialize(final Bootstrap<SuripuAdminConfiguration> bootstrap) {
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addBundle(new KinesisLoggerBundle<SuripuAdminConfiguration>() {
            @Override
            public KinesisLoggerConfiguration getConfiguration(final SuripuAdminConfiguration configuration) {
                return configuration.getKinesisLoggerConfiguration();
            }
        });
    }

    @Override
    public void run(SuripuAdminConfiguration configuration, Environment environment) throws Exception {
        final DBIFactory factory = new DBIFactory();
        final DBI commonDB = factory.build(environment, configuration.getCommonDB(), "postgresql");
        final AccountDAO accountDAO = commonDB.onDemand(AccountDAOImpl.class);

        environment.addResource(new PingResource());
        environment.addResource(new AccountResource(accountDAO));
    }
}

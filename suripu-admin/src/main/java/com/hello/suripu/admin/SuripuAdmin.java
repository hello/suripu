package com.hello.suripu.admin;

import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.hello.suripu.admin.resources.v1.AccountResource;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jdbi.DBIFactory;
import org.skife.jdbi.v2.DBI;

public class SuripuAdmin extends Service<SuripuAdminConfiguration> {

    public static void main(String[] args) throws Exception {
        new SuripuAdmin().run(args);
    }
    @Override
    public void initialize(Bootstrap<SuripuAdminConfiguration> bootstrap) {

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

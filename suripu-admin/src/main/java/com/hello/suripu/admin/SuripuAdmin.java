package com.hello.suripu.admin;

import com.hello.dropwizard.mikkusu.resources.PingResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class SuripuAdmin extends Service<SuripuAdminConfiguration> {

    public static void main(String[] args) throws Exception {
        new SuripuAdmin().run(args);
    }
    @Override
    public void initialize(Bootstrap<SuripuAdminConfiguration> bootstrap) {

    }

    @Override
    public void run(SuripuAdminConfiguration configuration, Environment environment) throws Exception {

        environment.addResource(new PingResource());
    }
}

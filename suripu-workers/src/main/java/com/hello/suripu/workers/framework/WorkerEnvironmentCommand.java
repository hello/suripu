package com.hello.suripu.workers.framework;

import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.validation.Validator;
import net.sourceforge.argparse4j.inf.Namespace;

public abstract class WorkerEnvironmentCommand<T extends WorkerConfiguration> extends ConfiguredCommand<T>{


    protected WorkerEnvironmentCommand(final String name, final String description) {
        super(name, description);
    }

    @Override
    protected void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration) throws Exception {
        final Environment environment = new Environment(bootstrap.getName(),
                configuration,
                bootstrap.getObjectMapperFactory().copy(),
                new Validator());
        bootstrap.runWithBundles(configuration, environment);
        run(environment, namespace, configuration);
    }

    protected abstract void run(Environment environment, Namespace namespace, T configuration) throws Exception;
}

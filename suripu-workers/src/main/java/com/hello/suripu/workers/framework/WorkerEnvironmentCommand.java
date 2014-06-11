package com.hello.suripu.workers.framework;

import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.validation.Validator;
import net.sourceforge.argparse4j.inf.Namespace;

public abstract class WorkerEnvironmentCommand<T extends Configuration> extends ConfiguredCommand<T>{

    private final Worker<T> worker;

    protected WorkerEnvironmentCommand(final Worker<T> worker, final String name, final String description) {
        super(name, description);
        this.worker = worker;
    }

    @Override
    protected final void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration) throws Exception {
        final Environment environment = new Environment(bootstrap.getName(),
                configuration,
                bootstrap.getObjectMapperFactory().copy(),
                new Validator());
        bootstrap.runWithBundles(configuration, environment);
        run(environment, namespace, configuration);
    }

    /**
     * Runs the command with the given {@link Environment} and {@link Configuration}.
     *
     * @param environment   the configured environment
     * @param namespace     the parsed command line namespace
     * @param configuration the configuration object
     * @throws Exception if something goes wrong
     */
    protected abstract void run(Environment environment, Namespace namespace, T configuration) throws Exception;
}

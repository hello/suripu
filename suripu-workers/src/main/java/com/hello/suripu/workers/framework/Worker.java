package com.hello.suripu.workers.framework;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.cli.Cli;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.LoggingFactory;
import com.yammer.dropwizard.util.Generics;

public abstract class Worker<T extends Configuration> {

    private class ShellService<T> extends Service {

        @Override
        public void initialize(Bootstrap bootstrap) {

        }

        @Override
        public void run(Configuration configuration, Environment environment) throws Exception {

        }
    }

    static {
        // make sure spinning up Hibernate Validator doesn't yell at us
        LoggingFactory.bootstrap();
    }

    /**
     * Returns the {@link Class} of the configuration class type parameter.
     *
     * @return the configuration class
     * @see com.yammer.dropwizard.util.Generics#getTypeParameter(Class, Class)
     */
    public final Class<T> getConfigurationClass() {
        return Generics.getTypeParameter(getClass(), Configuration.class);
    }

    /**
     * Initializes the service bootstrap.
     *
     * @param bootstrap the service bootstrap
     */
    public abstract void initialize(Bootstrap<T> bootstrap);

    /**
     * Parses command-line arguments and runs the service. Call this method from a {@code public
     * static void main} entry point in your application.
     *
     * @param arguments the command-line arguments
     * @throws Exception if something goes wrong
     */
    public final void run(String[] arguments) throws Exception {
        final Bootstrap<T> bootstrap = new Bootstrap<T>(new ShellService<T>());
        initialize(bootstrap);
        final Cli cli = new Cli(this.getClass(), bootstrap);
        cli.run(arguments);
    }
}

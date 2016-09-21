package com.hello.suripu.core.db.util;

import com.google.common.collect.ImmutableList;
import org.skife.jdbi.v2.ContainerBuilder;
import org.skife.jdbi.v2.tweak.ContainerFactory;

// See
// https://github.com/dropwizard/dropwizard/blob/master/dropwizard-jdbi/src/main/java/io/dropwizard/jdbi/ImmutableListContainerFactory.java

public class HelloTestImmutableListContainerFactory implements ContainerFactory<ImmutableList<?>> {
    @Override
    public boolean accepts(Class<?> type) {
        return ImmutableList.class.isAssignableFrom(type);
    }

    @Override
    public ContainerBuilder<ImmutableList<?>> newContainerBuilderFor(Class<?> type) {
        return new ImmutableListContainerBuilder();
    }

    private static class ImmutableListContainerBuilder implements ContainerBuilder<ImmutableList<?>> {
        private final ImmutableList.Builder<Object> builder = ImmutableList.builder();

        @Override
        public ContainerBuilder<ImmutableList<?>> add(Object it) {
            builder.add(it);
            return this;
        }

        @Override
        public ImmutableList<?> build() {
            return builder.build();
        }
    }
}

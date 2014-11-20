package com.hello.suripu.core;

import dagger.ObjectGraph;

public class ObjectGraphRoot {
    private static ObjectGraphRoot ourInstance = new ObjectGraphRoot();

    private ObjectGraph objectGraph;


    public static ObjectGraphRoot getInstance() {
        return ourInstance;
    }

    private ObjectGraphRoot() {

    }

    public void init(Object... modules) {
        this.objectGraph = ObjectGraph.create((Object[]) modules);
    }

    public <T> T inject(T instance) {
        return objectGraph.inject(instance);
    }
}

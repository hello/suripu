package com.hello.suripu.core.actions;

/**
 * Created by ksg on 2/2/17
 */
public interface ActionProcessor {
    Boolean add(Action action);
    int bufferSize();
    int maxBufferSize();
}
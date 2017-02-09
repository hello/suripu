package com.hello.suripu.core.actions;

/**
 * Created by ksg on 2/9/17
 */
public class ActionProcessorNoop implements ActionProcessor {
    @Override
    public Boolean add(Action action) {
        return true;
    }

    @Override
    public int bufferSize() {
        return 0;
    }

    @Override
    public int maxBufferSize() {
        return 0;
    }
}

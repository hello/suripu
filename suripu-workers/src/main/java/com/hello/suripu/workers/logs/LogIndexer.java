package com.hello.suripu.workers.logs;

public interface LogIndexer<T> {

    public void collect(final T t);
    public Integer index();
}

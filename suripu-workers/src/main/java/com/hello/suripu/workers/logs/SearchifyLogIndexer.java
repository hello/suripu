package com.hello.suripu.workers.logs;

public interface SearchifyLogIndexer<T> {

    public void collect(final T t);
    public Integer index();
}



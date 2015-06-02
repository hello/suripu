package com.hello.suripu.core.util;

/**
 * Created by benjo on 5/27/15.
 */
public interface Scorer<ValueType> {
    public ValueType getScore(final ValueType o1, final ValueType o2);
}


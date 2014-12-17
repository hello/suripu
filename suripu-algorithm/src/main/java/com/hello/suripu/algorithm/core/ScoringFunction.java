package com.hello.suripu.algorithm.core;

import java.util.Collection;
import java.util.Map;

/**
 * Created by pangwu on 12/16/14.
 */
public interface ScoringFunction<T, V> {
    Map<T, V> getPDF(final Collection<T> data);
    V getScore(final T data, final Map<T, V> pdf);
}

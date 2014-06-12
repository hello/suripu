package com.hello.suripu.algorithm.core;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

/**
 * Created by pangwu on 5/22/14.
 */
public interface DataSource<T> {
    ImmutableList<T> getDataForDate(final DateTime day);
}

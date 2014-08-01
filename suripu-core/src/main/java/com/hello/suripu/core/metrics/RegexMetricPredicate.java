package com.hello.suripu.core.metrics;

import com.google.common.collect.ImmutableList;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class RegexMetricPredicate implements MetricPredicate {

    public final List<String> substrings;

    public RegexMetricPredicate(List<String> substrings) {
        checkNotNull(substrings, "Substring can not be null");
        this.substrings = ImmutableList.copyOf(substrings);
    }

    @Override
    public boolean matches(MetricName name, Metric metric) {

        for(final String substring : substrings) {
            if(name.toString().contains(substring)) {
                return true;
            }
        }
        return false;
    }
}

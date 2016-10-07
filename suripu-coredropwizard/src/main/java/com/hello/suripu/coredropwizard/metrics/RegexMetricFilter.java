package com.hello.suripu.coredropwizard.metrics;

import com.google.common.collect.ImmutableList;


import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class RegexMetricFilter implements MetricFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegexMetricFilter.class);

    public final List<String> substrings;

    public RegexMetricFilter(List<String> substrings) {
        checkNotNull(substrings, "Substring can not be null");
        this.substrings = ImmutableList.copyOf(substrings);
    }

    @Override
    public boolean matches(String name, Metric metric) {
        if(name == null) {
            LOGGER.warn("warning=null_metric_name metric={}", metric.toString());
            return false;
        }
        for(final String substring : substrings) {
            if(name.contains(substring)) {
                return true;
            }
        }
        return false;
    }
}

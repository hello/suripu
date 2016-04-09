package com.hello.suripu.coredw.metrics;

import com.google.common.collect.ImmutableList;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class RegexMetricPredicate implements MetricPredicate {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegexMetricPredicate.class);

    public final List<String> substrings;

    public RegexMetricPredicate(List<String> substrings) {
        checkNotNull(substrings, "Substring can not be null");
        this.substrings = ImmutableList.copyOf(substrings);
    }

    @Override
    public boolean matches(MetricName name, Metric metric) {
        if(name == null) {
            LOGGER.warn("name should not be null");
            return false;
        }
        for(final String substring : substrings) {
            if(name.toString().contains(substring)) {
                return true;
            }
        }
        return false;
    }
}

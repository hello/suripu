package com.hello.suripu.core.metrics;

import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;

import java.util.regex.Pattern;

public class RegexMetricPredicate implements MetricPredicate {

    public final Pattern pattern;

    public RegexMetricPredicate(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public boolean matches(MetricName name, Metric metric) {
        boolean ok = pattern.matcher(name.getGroup()).matches();
        return ok;
    }
}

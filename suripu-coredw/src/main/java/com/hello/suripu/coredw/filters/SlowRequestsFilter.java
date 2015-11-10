package com.hello.suripu.coredw.filters;


import com.hello.suripu.core.util.HelloHttpHeader;
import com.yammer.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.yammer.dropwizard.util.Servlets.getFullUrl;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * A servlet filter which logs the methods and URIs of requests which take longer than a given
 * duration of time to complete.
 */
@SuppressWarnings("UnusedDeclaration")
public class SlowRequestsFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlowRequestsFilter.class);
    private final long threshold;

    /**
     * Creates a filter which logs requests which take longer than 1 second.
     */
    public SlowRequestsFilter() {
        this(Duration.seconds(1));
    }

    /**
     * Creates a filter which logs requests which take longer than the given duration.
     *
     * @param threshold    the threshold for considering a request slow
     */
    public SlowRequestsFilter(Duration threshold) {
        this.threshold = threshold.toNanoseconds();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { /* unused */ }

    @Override
    public void destroy() { /* unused */ }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final long startTime = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            final long elapsedNS = System.nanoTime() - startTime;
            final long elapsedMS = NANOSECONDS.toMillis(elapsedNS);
            final String senseId = (req.getHeader(HelloHttpHeader.SENSE_ID) == null) ? "unknown": req.getHeader(HelloHttpHeader.SENSE_ID);

            if (elapsedNS >= threshold) {
                LOGGER.warn("SLOW request: {} {} {} ({}ms)",
                        req.getMethod(),
                        getFullUrl(req), senseId, elapsedMS);
            }
        }
    }
}
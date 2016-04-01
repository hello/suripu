package com.hello.suripu.app;

import com.google.common.collect.Lists;
import com.librato.rollout.RolloutAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class LogResponseFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogResponseFilter.class);

    private static class ByteArrayPrintWriter {

        private final ByteArrayOutputStream baos;
        private final PrintWriter pw;
        private final ServletOutputStream sos;

        private ByteArrayPrintWriter(final ByteArrayOutputStream baos) {
            this.baos = baos;
            this.pw = new PrintWriter(baos);
            this.sos = new ByteArrayServletStream(baos);
        }

        public PrintWriter getWriter() {
            return pw;
        }

        public ServletOutputStream getStream() {
            return sos;
        }

        byte[] toByteArray() {
            return baos.toByteArray();
        }
    }

    private static class ByteArrayServletStream extends ServletOutputStream {

        private final ByteArrayOutputStream baos;

        private ByteArrayServletStream(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        public void write(int param) throws IOException {
            baos.write(param);
        }
    }



    private boolean shouldLog(final String ipAddress) {
        if(true) {
            return true;
        }
        return adapter.deviceFeatureActive("log_response_filter_by_ip", ipAddress, Lists.newArrayList("0:0:0:0:0:0:0:1"));
    }


    private final RolloutAdapter adapter;

    public LogResponseFilter(final RolloutAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse resp = (HttpServletResponse) response;
        final String ip = req.getRemoteAddr();

        if (!shouldLog(ip)) {
            chain.doFilter(request, response);
        } else {
            final ByteArrayPrintWriter pw = new ByteArrayPrintWriter(new ByteArrayOutputStream());
            final HttpServletResponse wrappedResp = new HttpServletResponseWrapper(resp) {
                public PrintWriter getWriter() {
                    return pw.getWriter();
                }

                public ServletOutputStream getOutputStream() {
                    return pw.getStream();
                }
            };

            try {
                chain.doFilter(request, wrappedResp);
            } finally {
                final byte[] bytes = pw.toByteArray();
                LOGGER.info(new String(bytes));
                response.getOutputStream().write(bytes); // write it back to the response stream. ugh
            }
        }
    }

    @Override
    public void destroy() {

    }

}

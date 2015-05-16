package com.hello.suripu.core.logging;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.lang.management.ManagementFactory;
import java.util.UUID;

/**
 * Created by benjo on 4/23/15.
 */
public class LoggerWithSessionId implements Logger {
    private final String uniqeString;

    private String getToken(final String extraInfo) {
        //UID as token
        String uuidString=  UUID.randomUUID().toString();

        if (extraInfo != null && !extraInfo.isEmpty()) {
            uuidString += " " + extraInfo;
        }
        return  uuidString;
    }

    private String prependToken(final String s) {
        return uniqeString + " " + s;
    }

    final public Logger logger;

    public LoggerWithSessionId(final Logger original) {
        logger = original;
        uniqeString = "";

    }

    public LoggerWithSessionId(final Logger original,final UUID uuid) {
        logger = original;
        uniqeString = uuid.toString();
    }


    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        logger.trace(prependToken(s));
    }

    @Override
    public void trace(String s, Object o) {
        logger.trace(prependToken(s),o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        logger.trace(prependToken(s),o,o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        logger.trace(prependToken(s),objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logger.trace(prependToken(s),throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String s) {
         logger.trace(marker,prependToken(s));
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        logger.trace(marker,prependToken(s),o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        logger.trace(marker,prependToken(s),o,o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        logger.trace(marker,prependToken(s),objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        logger.trace(marker,prependToken(s),throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        logger.debug(prependToken(s));
    }

    @Override
    public void debug(String s, Object o) {
        logger.debug(prependToken(s),o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        logger.debug(prependToken(s),o,o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        logger.debug(prependToken(s),objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logger.debug(prependToken(s),throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String s) {
        logger.debug(marker,prependToken(s));
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        logger.debug(marker,prependToken(s),o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        logger.debug(marker,prependToken(s),o,o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        logger.debug(marker,prependToken(s),objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        logger.debug(marker,prependToken(s),throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        logger.info(prependToken(s));
    }

    @Override
    public void info(String s, Object o) {
        logger.info(prependToken(s),o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        logger.info(prependToken(s),o,o1);
    }

    @Override
    public void info(String s, Object... objects) {
        logger.info(prependToken(s),objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        logger.info(prependToken(s),throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        logger.info(marker,prependToken(s));
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        logger.info(marker,prependToken(s),o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        logger.info(marker,prependToken(s),o,o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        logger.info(marker,prependToken(s),objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        logger.info(marker,prependToken(s),throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        logger.warn(prependToken(s));
    }

    @Override
    public void warn(String s, Object o) {
        logger.warn(prependToken(s),o);
    }

    @Override
    public void warn(String s, Object... objects) {
        logger.warn(prependToken(s),objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        logger.warn(prependToken(s),o,o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(prependToken(s),throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String s) {
        logger.warn(marker,prependToken(s));
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        logger.warn(marker,prependToken(s),o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        logger.warn(marker,prependToken(s),o,o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        logger.warn(marker,prependToken(s),objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        logger.warn(marker,prependToken(s),throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
         logger.error(prependToken(s));
    }

    @Override
    public void error(String s, Object o) {
        logger.error(prependToken(s),o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        logger.error(prependToken(s),o,o1);
    }

    @Override
    public void error(String s, Object... objects) {
        logger.error(prependToken(s),objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        logger.error(prependToken(s),throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        logger.error(marker,prependToken(s));
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        logger.error(marker,prependToken(s),o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        logger.error(marker,prependToken(s),o,o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        logger.error(marker,prependToken(s),objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        logger.error(marker,prependToken(s),throwable);
    }
}

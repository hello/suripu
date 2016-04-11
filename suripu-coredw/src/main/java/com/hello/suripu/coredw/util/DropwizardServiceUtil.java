package com.hello.suripu.coredw.util;

import com.sun.jersey.api.core.ResourceConfig;

import javax.ws.rs.ext.ExceptionMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DropwizardServiceUtil {

    public static void deregisterDWSingletons(ResourceConfig jerseyConfig) {
        final Set<Object> dwSingletons = jerseyConfig.getSingletons();
        final List<Object> singletonsToRemove = new ArrayList<Object>();

        for (final Object s : dwSingletons) {
            if (s instanceof ExceptionMapper && (s.getClass().getName().startsWith("com.yammer.dropwizard.jersey.") ||
                    s.getClass().getName().startsWith("com.yammer.dropwizard.auth")) || s.getClass().getName().startsWith("com.yammer.dropwizard.jdbi")) {
                singletonsToRemove.add(s);
            }
        }

        for (Object s : singletonsToRemove) {
            jerseyConfig.getSingletons().remove(s);
        }
    }
}

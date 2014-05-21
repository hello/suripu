package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;

public class Util {
    private static final String PREFIX = "bearer";

    /**
     * Extracts the bearer token from the header string
     * @param header
     * @return
     */
    public static Optional<String> extractBearerToken(final String header) {
        if (header == null) {
            return Optional.absent();
        }

        final int space = header.indexOf(' ');
        if (space <= 0) {
            return Optional.absent();
        }

        final String method = header.substring(0, space);
        if (!PREFIX.equalsIgnoreCase(method)) {
            return Optional.absent();
        }

        final String bearer = header.substring(space + 1);
        return Optional.of(bearer);
    }
}

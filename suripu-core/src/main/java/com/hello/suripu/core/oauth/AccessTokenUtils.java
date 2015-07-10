package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AccessTokenUtils {


    private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenUtils.class);

    /**
     * Token format for client is {appId}.{uuidWithoutHyphens}
     * @param dirtyToken
     * @return
     */
    public static Optional<UUID> cleanUUID(final String dirtyToken) {
        // TODO: make sure this is efficient enough
        final int dotIndex = dirtyToken.indexOf('.');
        if(dotIndex == -1) {
            LOGGER.error("AccessToken is missing a (.) character: {}", dirtyToken);
            return Optional.absent();
        }

        final String uuidWithoutAppId = dirtyToken.substring(dotIndex + 1);

        final String uuidWithHyphens =  uuidWithoutAppId.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{8})", "$1-$2-$3-$4-$5" );
        try {
            return Optional.of(UUID.fromString(uuidWithHyphens));
        } catch (IllegalArgumentException e) {
            LOGGER.error("Could not convert {} to UUID. Exception: {}", uuidWithHyphens, e.getMessage());
        }

        return Optional.absent();

    }

    /**
     * Attempts to extract ApplicationId from token string
     * @param dirtyToken
     * @return
     */
    public static Optional<Long> extractAppIdFromToken(final String dirtyToken) {
        final int dotIndex = dirtyToken.indexOf('.');
        if(dotIndex == -1) {
            LOGGER.error("AccessToken is missing a (.) character: {}", dirtyToken);
            return Optional.absent();
        }

        try {
            return Optional.of(Long.parseLong(dirtyToken.substring(0, dotIndex)));
        } catch (IllegalArgumentException e) {
            LOGGER.error("Could not extract Long applicationId from token {}. Reason: {}", dirtyToken, e.getMessage());
        }

        return Optional.absent();
    }

    public static Integer expiresInDays(final AccessToken accessToken) {
        return Days.daysBetween(DateTime.now(DateTimeZone.UTC), accessToken.createdAt.plusSeconds((int)(long) accessToken.expiresIn)).getDays();
    }
}

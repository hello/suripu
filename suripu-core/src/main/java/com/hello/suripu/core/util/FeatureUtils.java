package com.hello.suripu.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 6/1/15.
 */
public class FeatureUtils {

    public static final Integer MAX_ROLLOUT_VALUE = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureUtils.class);

    public static Boolean entityIdHashInPercentRange(final String entityId, final Integer startRange, final Integer endRange) {

        if (startRange > endRange) {
            LOGGER.error("Invalid range parameters.");
            return false;
        }
        if (endRange > MAX_ROLLOUT_VALUE) {
            LOGGER.error("End range value greater than max allowed value.");
            return false;
        }

        final Integer remainder = Math.abs(entityId.hashCode()) % MAX_ROLLOUT_VALUE;
        return ((startRange <= remainder) && (remainder < endRange));
    }
}

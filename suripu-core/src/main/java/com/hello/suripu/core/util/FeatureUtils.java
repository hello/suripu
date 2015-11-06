package com.hello.suripu.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 6/1/15.
 */
public class FeatureUtils {

    public static final Float MAX_ROLLOUT_VALUE = 100.00f;
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureUtils.class);

    public static Boolean entityIdHashInPercentRange(final String entityId, final Float startRange, final Float endRange) {

        if (startRange > endRange) {
            LOGGER.error("Invalid range parameters.");
            return false;
        }
        if (endRange > MAX_ROLLOUT_VALUE) {
            LOGGER.error("End range value greater than max allowed value.");
            return false;
        }

        final Float entityPercent = (Math.abs(entityId.hashCode()) % (MAX_ROLLOUT_VALUE * 100.00f) / 100.0f);
        return ((startRange <= entityPercent) && (entityPercent <= endRange));
    }
}

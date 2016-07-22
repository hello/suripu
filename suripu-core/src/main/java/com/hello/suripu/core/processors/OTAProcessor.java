package com.hello.suripu.core.processors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by jnorgan on 3/3/15.
 */
public class OTAProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(OTAProcessor.class);

    public static Boolean canDeviceOTA(final String deviceID,
                                       final List<String> deviceGroups,
                                       final Set<String> overrideOTAGroups,
                                       final Integer deviceUptimeDelayMinutes,
                                       final Integer uptimeInSeconds,
                                       final DateTime currentDTZ,
                                       final DateTime startOTAWindow,
                                       final DateTime endOTAWindow,
                                       final Boolean bypassOTAChecks,
                                       final String ipAddress) {

        boolean canOTA;

        if (bypassOTAChecks) {
            LOGGER.info("OTA checks are bypassed for device: {}", deviceID);
            canOTA = true;
        } else {

            //Allow OTA Updates only in config-defined update window
            if (currentDTZ.isAfter(startOTAWindow) && currentDTZ.isBefore(endOTAWindow)) {
                canOTA = true;
                LOGGER.debug("Device within OTAU window.");
            } else {
                canOTA = false;
                LOGGER.debug("Device outside OTAU window.");
            }

            //Has the device been running long enough to receive an OTA Update?
            if (uptimeInSeconds != -1)
            {
                if (!(uptimeInSeconds > deviceUptimeDelayMinutes * DateTimeConstants.SECONDS_PER_MINUTE)) {
                    canOTA = false;
                    LOGGER.debug("Device failed up-time check.");
                }
            } else {
                LOGGER.error("No up-time provided in batch for device: {}", deviceID);
                canOTA = false;
            }

            //Check for overrideOTAGroups as defined in the OTA configuration
            if (!Collections.disjoint(deviceGroups, overrideOTAGroups)) {
                canOTA = true;
                LOGGER.debug("Device belongs to OTAU check override group");
            }
        }
        return canOTA;
    }
}

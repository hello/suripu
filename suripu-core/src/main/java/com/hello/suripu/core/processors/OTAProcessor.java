package com.hello.suripu.core.processors;

import com.google.common.net.InetAddresses;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                                       final Boolean isAlwaysOTA,
                                       final String ipAddress) {

        boolean canOTA;

        //PCH IP address range exclusion
        try {
            final Integer ipAdd = InetAddresses.coerceToInteger(InetAddresses.forString(ipAddress));
            final Integer startRange1 = InetAddresses.coerceToInteger(InetAddresses.forString("203.166.220.233"));
            final Integer endRange1 = InetAddresses.coerceToInteger(InetAddresses.forString("203.166.220.246"));
            final Integer startRange2 = InetAddresses.coerceToInteger(InetAddresses.forString("116.204.105.25"));
            final Integer endRange2 = InetAddresses.coerceToInteger(InetAddresses.forString("116.204.105.38"));

            if ((startRange1 <= ipAdd && ipAdd <= endRange1) ||
                    (startRange2 <= ipAdd && ipAdd <= endRange2)) {
                LOGGER.debug("IP Address Found in PCH Range: {}.", ipAddress);
                return false;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid IP string used in PCH exclusion check. '{}'", ipAddress);
        }

        if (isAlwaysOTA) {
            LOGGER.debug("Always OTA is on for device: ", deviceID);
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

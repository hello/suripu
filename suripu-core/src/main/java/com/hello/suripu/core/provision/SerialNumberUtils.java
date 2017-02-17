package com.hello.suripu.core.provision;

import com.google.common.base.Optional;
import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.device.v2.Sense;

public class SerialNumberUtils {

    // August 1st
    //    910-00100
    //    PRODUCT ASSY, SENSE 1.5 DVT, WHITE
    //
    //    910-00101
    //    PRODUCT ASSY, SENSE 1.5 DVT, Black

    private final static String SENSE_ONE_FIVE_WHITE = "91000100";
    private final static String SENSE_ONE_FIVE_BLACK = "91000101";
    private final static Integer MIN_SN_LENGTH = SENSE_ONE_FIVE_BLACK.length();

    private final static String SENSE_ONE_WHITE = "91000008W";
    private final static String SENSE_ONE_BLACK = "91000008B";

    public static HardwareVersion fromSerialNumber(final String serialNumber) {
        if(serialNumber != null && serialNumber.length() > MIN_SN_LENGTH) {
            if (serialNumber.startsWith(SENSE_ONE_FIVE_BLACK) || serialNumber.startsWith(SENSE_ONE_FIVE_WHITE)) {
                return HardwareVersion.SENSE_ONE_FIVE;
            }
            else if(serialNumber.startsWith(SENSE_ONE_BLACK) || serialNumber.startsWith(SENSE_ONE_WHITE))  {
                return HardwareVersion.SENSE_ONE;
            }
        }

        throw new IllegalArgumentException(String.format("invalid sn=%s", serialNumber));
    }

    public static Optional<Sense.Color> extractColorFrom(final String serialNumber) {
        if(serialNumber.startsWith(SENSE_ONE_BLACK) || serialNumber.startsWith(SENSE_ONE_FIVE_BLACK)) {
            return Optional.of(Sense.Color.BLACK);
        } else if(serialNumber.startsWith(SENSE_ONE_WHITE) || serialNumber.startsWith(SENSE_ONE_FIVE_WHITE)) {
            return Optional.of(Sense.Color.WHITE);
        }

        return Optional.absent();
    }
}

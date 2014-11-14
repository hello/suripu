package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.api.input.DataInputProtos.periodic_data;
import org.apache.commons.codec.binary.Hex;

/**
 * Created by pangwu on 10/16/14.
 */
public class DeviceIdUtil {
    public static Optional<String> macToStringId(final byte[] mac){
        if(mac.length != 6){
            return Optional.absent();
        }

        // I made this to keep backward compatibility
        return Optional.of(new String(Hex.encodeHex(mac)).toLowerCase());  // Always lower case!

    }


    public static Optional<String> getMorpheusId(final periodic_data morpheusData){
        if(!morpheusData.hasDeviceId()){
            if(!morpheusData.hasMac()){
                return Optional.absent();
            }

            // Just for backward compatibility
            // This is a VERY BAD idea, the firmware should own and black box the knowledge
            // of what is a deviceId and how to encoded it to searchable plain text.
            // Or we will tightly couple the backend to firmware, which will bring endless trouble
            // if we want to use the same protobuf between different hardware.
            return macToStringId(morpheusData.getMac().toByteArray());
        }

        return Optional.of(morpheusData.getDeviceId());
    }
}

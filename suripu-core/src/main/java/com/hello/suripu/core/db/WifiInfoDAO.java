package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.WifiInfo;


public interface WifiInfoDAO {
    /**
     * Retrieves wifi info per sense
     * @param senseId String
     * @return WifiInfo
     */
    Optional<WifiInfo> get(String senseId);

    /**
     *
     * @param wifiInfo WifiInfo
     * @return int
     */
    Boolean put(WifiInfo wifiInfo);
}

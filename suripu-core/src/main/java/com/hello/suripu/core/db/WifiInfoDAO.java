package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.WifiInfo;

import java.util.List;


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
     * @return Boolean
     */
    Boolean put(WifiInfo wifiInfo);
    Boolean putBatch(List<WifiInfo> wifiInfoList);

}

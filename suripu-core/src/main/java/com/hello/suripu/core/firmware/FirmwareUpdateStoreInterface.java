package com.hello.suripu.core.firmware;

public interface FirmwareUpdateStoreInterface {
    FirmwareUpdate getFirmwareFilesForCacheKey(FirmwareCacheKey cacheKey, SenseFirmwareUpdateQuery query);
    FirmwareUpdate getFirmwareUpdate(SenseFirmwareUpdateQuery senseQuery);
}

package com.hello.suripu.core.firmware;

public interface FirmwareUpdateStoreInterface {
    FirmwareUpdate getFirmwareFilesForS3ObjectKey(FirmwareCacheKey cacheKey, SenseFirmwareUpdateQuery query);
    FirmwareUpdate getFirmwareUpdate(SenseFirmwareUpdateQuery senseQuery);
}

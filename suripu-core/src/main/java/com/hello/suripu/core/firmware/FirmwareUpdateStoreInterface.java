package com.hello.suripu.core.firmware;

public interface FirmwareUpdateStoreInterface {
    FirmwareUpdate getFirmwareFilesForS3ObjectKey(String objectKey, SenseFirmwareUpdateQuery query);
    FirmwareUpdate getFirmwareUpdate(SenseFirmwareUpdateQuery senseQuery);
}

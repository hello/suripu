package com.hello.suripu.core.firmware.db;

import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.firmware.FirmwareFile;
import com.hello.suripu.core.firmware.HardwareVersion;

import java.util.Map;

public interface OTAFileSettingsDAO {
    ImmutableMap<String, FirmwareFile> mappingForHardwareVersion(HardwareVersion hardwareVersion);
    boolean put(HardwareVersion hardwareVersion, Map<String,FirmwareFile> files);
}

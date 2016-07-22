package com.hello.suripu.core.firmware;

import com.google.common.base.Objects;
import com.hello.suripu.api.output.OutputProtos;

import java.util.ArrayList;
import java.util.List;


public class FirmwareUpdate {
    public final static String MISSING_FIRMWARE_UPDATE_VERSION = "0";

    public final String firmwareVersion;
    public List<OutputProtos.SyncResponse.FileDownload> files = new ArrayList<OutputProtos.SyncResponse.FileDownload>();

    private FirmwareUpdate(final String firmwareVersion, final List<OutputProtos.SyncResponse.FileDownload> files) {
        this.firmwareVersion = firmwareVersion;
        this.files.addAll(files);
    }

    public static FirmwareUpdate create(final String firmwareVersion, List<OutputProtos.SyncResponse.FileDownload> files) {
        return new FirmwareUpdate(firmwareVersion, files);
    }

    public static FirmwareUpdate missing() {
        return new FirmwareUpdate(MISSING_FIRMWARE_UPDATE_VERSION, new ArrayList<OutputProtos.SyncResponse.FileDownload>());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FirmwareUpdate)) {
            return false;
        }

        final FirmwareUpdate other = (FirmwareUpdate) obj;
        return Objects.equal(firmwareVersion, other.firmwareVersion) &&
                Objects.equal(files, other.files);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(firmwareVersion, files);
    }
}

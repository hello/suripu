package com.hello.suripu.core.models.sleep_sounds;

import com.google.common.base.Objects;
import com.hello.suripu.core.firmware.HardwareVersion;

public class FilePathLookup {

    final public String name;
    final public HardwareVersion hardwareVersion;

    public FilePathLookup(String name, HardwareVersion hardwareVersion) {
        this.name = name;
        this.hardwareVersion = hardwareVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FilePathLookup)) {
            return false;
        }

        final FilePathLookup other = (FilePathLookup) obj;
        return Objects.equal(name, other.name) &&
                Objects.equal(hardwareVersion, other.hardwareVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, hardwareVersion);
    }
}

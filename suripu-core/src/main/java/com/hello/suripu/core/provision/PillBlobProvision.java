package com.hello.suripu.core.provision;

public class PillBlobProvision {
    public final String pillId;
    public final String key;
    public final String serialNumber;
    public final String blob;

    public PillBlobProvision(final String pillId, final String key, final String serialNumber, final String blob) {
        this.pillId = pillId.toUpperCase();
        this.key = key.toUpperCase();
        this.serialNumber = serialNumber.toUpperCase();
        this.blob = blob;
    }
}

package com.hello.suripu.core.util;

public class SenseProvision {

    public final String deviceIdHex;
    public final String aesKeyHex;
    public final String checkSumHex;


    private SenseProvision(final String deviceIdHex, final String aesKeyHex, final String checkSumHex) {
        this.deviceIdHex = deviceIdHex;
        this.aesKeyHex = aesKeyHex;
        this.checkSumHex = checkSumHex;
    }

    public static SenseProvision create(final String deviceHex, final String aesKeyHex, final String checkSumHex) {
        return new SenseProvision(deviceHex, aesKeyHex, checkSumHex);
    }
}

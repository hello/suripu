package com.hello.suripu.core.models;

import org.joda.time.DateTime;

public class PillProxData {

    public final Long accountId;
    public final String pillId;
    public final Integer proxValue;
    public final DateTime ts;
    public final Integer offsetMillis = 0;   // TODO: LOL

    private PillProxData(final Long accountId, final String pillId, final Integer proxValue, final DateTime ts) {
        this.accountId = accountId;
        this.pillId = pillId;
        this.proxValue = proxValue;
        this.ts = ts;
    }

    public static PillProxData create(final Long accountId, final String pillId, final Integer proxValue, final DateTime ts) {
        return new PillProxData(accountId, pillId, proxValue,ts);
    }

    public static PillProxData fromEncryptedData(final Long accountId, final byte[] encryptedProxData, final byte[] key) {
        return new PillProxData(0L, "yo", 0, DateTime.now());
    }
}

package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PairingInfo {

    @JsonProperty("sense_id")
    public final String senseExternalId;

    @JsonProperty("paired_accounts")
    public final Integer numberOfPairedAccounts;


    private PairingInfo(final String senseExternalId, final Integer numberOfPairedAccounts) {
        this.senseExternalId = senseExternalId;
        this.numberOfPairedAccounts = numberOfPairedAccounts;
    }

    public static PairingInfo create(final String senseExternalId, final Integer numberOfPairedAccounts) {
        return new PairingInfo(senseExternalId, numberOfPairedAccounts);
    }
}

package com.hello.suripu.core.accounts.pairings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.Account;

import java.util.Objects;
import java.util.UUID;

public class PairedAccount {

    private final String name;
    private final String externalId;
    private final boolean isSelf;



    private PairedAccount(final String name, final String externalId, final Boolean isSelf) {
        this.name = name;
        this.externalId = externalId;
        this.isSelf = isSelf;
    }

    @JsonProperty("id")
    public String id() {
        return externalId;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("is_self")
    public boolean isSelf() {
        return isSelf;
    }

    static PairedAccount from(final Account account, final Long requesterAccountId) {
        final boolean isSelf = account.id.isPresent() && account.id.get() == requesterAccountId;
        return new PairedAccount(account.firstname, account.extId(), isSelf);
    }


    static PairedAccount fromExtId(final String name, final String extId, boolean isSelf) {
        return new PairedAccount(name, extId, isSelf);
    }

    @JsonCreator
    public static PairedAccount create(
            @JsonProperty("id") String externalId,
            @JsonProperty("name") String name,
            @JsonProperty("is_self") boolean isSelf) {

        final String cleanName = Objects.toString(name, "");
        final String cleanId = UUID.fromString(externalId).toString();
        // it should be safe to ignore name
        return fromExtId(cleanName, cleanId, isSelf);
    }
}

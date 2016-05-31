package com.hello.suripu.core.passwordreset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hello.suripu.core.models.Account;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.UUID;

public class PasswordReset {
    @JsonIgnore
    public final Long accountId;


    public final String name;
    public final UUID uuid;
    public final String state;

    @JsonIgnore
    public final DateTime createdAt;

    private PasswordReset(final Long accountId, final String name, final UUID uuid, final String state, final DateTime createdAt) {
        this.accountId = accountId;
        this.name = name;
        this.uuid = uuid;
        this.state = state;
        this.createdAt = createdAt;
    }

    public static PasswordReset recreate(final Long accountId, final String name, final String uuid, final String state, final DateTime createdAt) {
        return new PasswordReset(accountId, name, UUID.fromString(uuid), state, createdAt);
    }

    public static PasswordReset create(final Account account) {
        final String state = DigestUtils.md5Hex(account.password);
        return new PasswordReset(account.id.get(), account.name(), UUID.randomUUID(), state, DateTime.now(DateTimeZone.UTC));
    }
}
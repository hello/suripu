package com.hello.suripu.core.models;

import org.joda.time.DateTime;

/**
 * Created by jarredheinrich on 5/26/16.
 */
public class AccountDate {
    final public Long accountId;
    final public DateTime created;


    public AccountDate(final Long accountId, final DateTime created) {
        this.accountId = accountId;
        this.created = created;
    }
}
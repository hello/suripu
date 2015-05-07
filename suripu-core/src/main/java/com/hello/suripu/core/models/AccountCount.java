package com.hello.suripu.core.models;


import org.joda.time.DateTime;

public class AccountCount {
    public final DateTime createdDate;
    public final Integer count;
    public AccountCount(final DateTime createdDate, final Integer count) {
        this.createdDate = createdDate;
        this.count = count;
    }
}

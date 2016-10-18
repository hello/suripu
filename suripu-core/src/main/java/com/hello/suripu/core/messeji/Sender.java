package com.hello.suripu.core.messeji;

public class Sender {
    private String id;

    private Sender(final String id) {
        this.id = id;
    }

    public static Sender fromAccountId(final Long accountId) {
        return new Sender(String.format("account:%s", accountId));
    }

    public String id() {
        return id;
    }
}

package com.hello.suripu.core.oauth;

public enum OAuthScope {

    USER_BASIC (0),
    USER_EXTENDED (1);

    private int value;

    private OAuthScope(int value) {
        this.value = value;
    }
}

package com.hello.suripu.core.models;

public enum ApiVersion {
    V1(1),
    V2(2);

    public final int code;

    ApiVersion(int code) {
        this.code = code;
    }
}

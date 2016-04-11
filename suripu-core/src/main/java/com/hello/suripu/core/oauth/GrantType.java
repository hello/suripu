package com.hello.suripu.core.oauth;

public enum GrantType {
    AUTH_CODE("code", 0),
    IMPLICIT("implicit", 1),
    PASSWORD("password", 2);

//        Support application refresh details?
//        CLIENT_CREDENTIALS("credentials")

    private String name;
    private int value;

    GrantType(String name, int value) {

    }

    public static GrantType fromString(String text) {
        if (text != null) {
            for (GrantType t : GrantType.values()) {
                if (text.equalsIgnoreCase(t.name())) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}

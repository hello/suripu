package com.hello.suripu.core.oauth;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class GrantTypeParam {
    private final String originalValue;
    private final GrantType type;

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
                    if (text.equalsIgnoreCase(t.name)) {
                        return t;
                    }
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }


    }

    public GrantTypeParam(String grantType) throws WebApplicationException {
        try {
            this.originalValue = grantType;
            this.type = GrantType.fromString(grantType);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(
                    Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Couldn't parse: " + grantType + " (" + e.getMessage() + ")")
                            .build()
            );
        }
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public GrantType getType() {
        return type;
    }
}

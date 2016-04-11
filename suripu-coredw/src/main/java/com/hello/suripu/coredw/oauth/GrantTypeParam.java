package com.hello.suripu.coredw.oauth;

import com.hello.suripu.core.oauth.GrantType;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class GrantTypeParam {
    private final String originalValue;
    private final GrantType type;

    public GrantTypeParam(String grantType) throws WebApplicationException {
        this.originalValue = grantType;
        try {
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

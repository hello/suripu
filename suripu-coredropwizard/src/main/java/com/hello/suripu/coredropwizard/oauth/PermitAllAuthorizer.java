package com.hello.suripu.coredropwizard.oauth;

import com.hello.suripu.core.oauth.OAuthScope;
import java.security.Principal;

/**
 * An {@link Authorizer} that grants access for any principal in any role.
 *
 * @param <P> the type of the principal
 */
public class PermitAllAuthorizer<P extends Principal> implements Authorizer<P> {

    @Override
    public boolean authorize(P principal, OAuthScope role) {
        return true;
    }
}
package com.hello.suripu.coredw8.oauth;

import com.hello.suripu.core.oauth.OAuthScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 8/12/15.
 */
public class OAuthAuthorizer implements Authorizer<AccessToken> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthAuthorizer.class);

    @Override
    public boolean authorize(AccessToken accessToken, OAuthScope role) {
        try {
            if(accessToken.hasScope(role)) {
                return true;
            }
        } catch (Exception ex) {
            LOGGER.error("OAuth role '{}' not found.", role);
            return false;
        }

        return false;
    }
}
package com.hello.suripu.coredw.oauth;

import com.yammer.dropwizard.auth.AuthenticationException;

/**
 * Created by jakepiccolo on 9/25/15.
 */
public class MissingRequiredScopeAuthenticationException extends AuthenticationException {
    public MissingRequiredScopeAuthenticationException(Throwable cause) { super(cause); }
}

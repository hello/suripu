package com.hello.suripu.coredw8.oauth;

import com.hello.suripu.core.oauth.OAuthScope;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by jnorgan on 8/21/15.
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface ScopesAllowed {
    OAuthScope[] value();
}

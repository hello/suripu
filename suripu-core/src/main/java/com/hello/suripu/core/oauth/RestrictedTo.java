package com.hello.suripu.core.oauth;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface RestrictedTo {
    OAuthScope[] value() default OAuthScope.USER_BASIC;
}

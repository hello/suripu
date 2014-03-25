package com.hello.suripu.core.oauth;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface Scope {

    OAuthScope[] value() default OAuthScope.USER_BASIC;
}

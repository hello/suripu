package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;

public interface ApplicationStore<A, B, C> {

    Optional<A> getApplication(C clientDetails, Long accountId);
    void register(B registration);
    void activateForAccountId(A application, Long accountId);
}

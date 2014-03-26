package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;

public interface ApplicationStore<A, C> {

    Optional<A> getApplication(C clientDetails, Long accountId);
    void storeApplication(A application);
    void activateForAccountId(A application, Long accountId);
}

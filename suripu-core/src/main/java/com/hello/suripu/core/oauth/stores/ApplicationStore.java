package com.hello.suripu.core.oauth.stores;

import com.google.common.base.Optional;

import java.util.List;

public interface ApplicationStore<A, B> {

    Optional<A> getApplicationById(Long applicationId);
    Optional<A> getApplicationByClientId(String clientId);
    A register(B registration);
    void activateForAccountId(A application, Long accountId);
    List<A> getApplicationsByDevId(Long accountId);
    List<A> getAll();
}

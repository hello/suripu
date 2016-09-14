package com.hello.suripu.coredropwizard.oauth.stores;

import com.google.common.base.Optional;

import java.util.List;

public interface ExternalApplicationStore<A> {

    Optional<A> getApplicationById(Long applicationId);
    Optional<A> getApplicationByClientId(String clientId);
    Optional<A> getApplicationByName(String applicationName);
    List<A> getAll();
}

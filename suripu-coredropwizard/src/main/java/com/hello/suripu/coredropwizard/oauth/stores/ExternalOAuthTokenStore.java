package com.hello.suripu.coredropwizard.oauth.stores;

import com.google.common.base.Optional;

import com.hello.suripu.coredropwizard.oauth.InvalidExternalTokenException;

public interface ExternalOAuthTokenStore<T> {

    void storeToken(T externalToken) throws InvalidExternalTokenException;

    Optional<T> getTokenByDeviceId(String deviceId, Long appId);

    void disable(T accessToken);

    void disableByRefreshToken(String refreshToken);

}

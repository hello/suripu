package com.hello.suripu.coredropwizard.db;

import com.google.common.base.Optional;

import com.hello.suripu.coredropwizard.db.binders.BindExternalToken;
import com.hello.suripu.coredropwizard.db.mappers.ExternalTokenMapper;
import com.hello.suripu.coredropwizard.oauth.ExternalToken;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;


@RegisterMapper(ExternalTokenMapper.class)
public interface ExternalTokenDAO {

    @SingleValueResult(ExternalToken.class)
    @SqlQuery("SELECT * FROM external_oauth_tokens WHERE access_token = :access_token")
    Optional<ExternalToken> getByAccessToken(@Bind("access_token") String accessToken);

    @SingleValueResult(ExternalToken.class)
    @SqlQuery("SELECT * FROM external_oauth_tokens WHERE refresh_token = :refresh_token")
    Optional<ExternalToken> getByRefreshToken(@Bind("refresh_token") String refreshToken);

    @SingleValueResult(ExternalToken.class)
    @SqlQuery("SELECT * FROM external_oauth_tokens WHERE device_id = :device_id AND app_id = :application_id AND access_expires_in > 0 ORDER BY created_at DESC LIMIT 1")
    Optional<ExternalToken> getByDeviceId(@Bind("device_id") String deviceId, @Bind("application_id") Long applicationId);

    @SqlUpdate("INSERT INTO external_oauth_tokens (access_token, refresh_token, access_expires_in, refresh_expires_in, app_id, device_id) VALUES (:access_token, :refresh_token, :access_expires_in, :refresh_expires_in, :app_id, :device_id)")
    void storeExternalToken(@BindExternalToken ExternalToken externalToken);

    @SqlUpdate("UPDATE external_oauth_tokens SET access_expires_in=0 WHERE access_token = :access_token")
    void disable(@Bind("access_token") String accessToken);

    @SqlUpdate("UPDATE external_oauth_tokens SET access_expires_in=0, refresh_expires_in=0 WHERE refresh_token = :refresh_token")
    void disableByRefreshToken(@Bind("refresh_token") String refreshToken);
}

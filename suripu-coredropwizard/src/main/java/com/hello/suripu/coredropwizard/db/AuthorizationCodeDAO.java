package com.hello.suripu.coredropwizard.db;

import com.google.common.base.Optional;

import com.hello.suripu.coredropwizard.db.binders.BindAuthorizationCode;
import com.hello.suripu.coredropwizard.db.mappers.AuthorizationCodeMapper;
import com.hello.suripu.coredropwizard.oauth.AuthorizationCode;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.util.UUID;


@RegisterMapper(AuthorizationCodeMapper.class)
public interface AuthorizationCodeDAO {

    @SingleValueResult(AuthorizationCode.class)
    @SqlQuery("SELECT * FROM oauth_codes WHERE auth_code = cast(:auth_code as uuid)")
    Optional<AuthorizationCode> getByAuthCode(@Bind("auth_code") UUID authorizationCode);

    @SqlUpdate("INSERT INTO oauth_codes (auth_code, expires_in, app_id, account_id, scopes) VALUES (:auth_code, :expires_in, :app_id, :account_id, :scopes)")
    void storeAuthCode(@BindAuthorizationCode AuthorizationCode authorizationCode);

    @SqlUpdate("UPDATE oauth_codes SET expires_in=0 WHERE auth_code = cast(:auth_code as uuid)")
    void disableByAuthCode(@Bind("auth_code") UUID authorizationCode);
}

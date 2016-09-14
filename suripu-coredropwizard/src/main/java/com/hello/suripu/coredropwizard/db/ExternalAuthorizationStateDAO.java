package com.hello.suripu.coredropwizard.db;

import com.google.common.base.Optional;

import com.hello.suripu.coredropwizard.db.binders.BindExternalAuthorizationState;
import com.hello.suripu.coredropwizard.db.mappers.ExternalAuthorizationStateMapper;
import com.hello.suripu.coredropwizard.oauth.ExternalAuthorizationState;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;


@RegisterMapper(ExternalAuthorizationStateMapper.class)
public interface ExternalAuthorizationStateDAO {

    @SingleValueResult(ExternalAuthorizationState.class)
    @SqlQuery("SELECT * FROM external_oauth_states WHERE auth_state = :auth_state")
    Optional<ExternalAuthorizationState> getByAuthState(@Bind("auth_state") String authorizationState);

    @SqlUpdate("INSERT INTO external_oauth_states (auth_state, app_id, device_id) VALUES (:auth_state, :app_id, :device_id)")
    void storeAuthCode(@BindExternalAuthorizationState ExternalAuthorizationState authorizationState);

}

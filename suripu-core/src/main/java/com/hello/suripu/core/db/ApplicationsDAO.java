package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindApplicationRegistration;
import com.hello.suripu.core.db.mappers.ApplicationMapper;
import com.hello.suripu.core.db.util.SqlArray;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.util.List;

@RegisterMapper(ApplicationMapper.class)
public interface ApplicationsDAO {

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO oauth_applications (name, client_id, client_secret, redirect_uri, scopes, dev_account_id, description, grant_type) " +
            "VALUES(:name, :client_id, :client_secret, :redirect_uri, :scopes, :dev_account_id, :description, :grant_type);")
    Long insertRegistration(@BindApplicationRegistration ApplicationRegistration applicationRegistration);

    @SqlQuery("SELECT * FROM oauth_applications WHERE id = :id")
    @SingleValueResult(Application.class)
    Optional<Application> getById(@Bind("id") Long applicationId);

    @SqlQuery("SELECT * FROM oauth_applications WHERE client_id = :client_id")
    @RegisterMapper(ApplicationMapper.class)
    @SingleValueResult(Application.class)
    Optional<Application> getByClientId(@Bind("client_id") String applicationClientId);

    @SqlQuery("SELECT * FROM oauth_applications WHERE dev_account_id = :dev_account_id")
    ImmutableList<Application> getAllByDevId(@Bind("dev_account_id") Long devAccountId);

    @SqlQuery("SELECT * FROM oauth_applications;")
    List<Application> getAll();

    @SqlQuery("SELECT count(*) FROM authorized_oauth_applications WHERE app_id = :app_id AND account_id = : account_id LIMIT 1;")
    int exists(@Bind("app_id") Long appId, @Bind("account_id") Long accountId);

    @SqlUpdate("INSERT INTO authorized_oauth_applications (app_id, account_id) VALUES(:app_id, :account_id);")
    void insertInstallation(@Bind("app_id") Long appId, @Bind("account_id") Long accountId);

    @SqlUpdate("UPDATE oauth_applications SET scopes = :scopes WHERE id = :id")
    void updateScopes(@Bind("scopes") SqlArray<Integer> scopes, @Bind("id") Long applicationId);
}

package com.hello.suripu.coredropwizard.db;

import com.google.common.base.Optional;

import com.hello.suripu.coredropwizard.db.binders.BindExternalApplicationData;
import com.hello.suripu.coredropwizard.db.mappers.ExternalApplicationDataMapper;
import com.hello.suripu.coredropwizard.oauth.ExternalApplicationData;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;


@RegisterMapper(ExternalApplicationDataMapper.class)
public interface ExternalApplicationDataDAO {

    @SingleValueResult(ExternalApplicationData.class)
    @SqlQuery("SELECT * FROM external_application_data WHERE app_id = :application_id AND device_id = :device_id")
    Optional<ExternalApplicationData> getAppData(@Bind("application_id") Long applicationId, @Bind("device_id") String deviceId);

    @SqlUpdate("INSERT INTO external_application_data (app_id, device_id, data) VALUES (:app_id, :device_id, :data)")
    void insertAppData(@BindExternalApplicationData ExternalApplicationData appData);

    @SqlUpdate("UPDATE external_application_data SET data = :data WHERE app_id = :app_id AND device_id = :device_id")
    void updateAppData(@BindExternalApplicationData ExternalApplicationData appData);

}

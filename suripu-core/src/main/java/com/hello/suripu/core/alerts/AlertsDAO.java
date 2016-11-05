package com.hello.suripu.core.alerts;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.util.List;

@RegisterMapper(AlertMapper.class)
public interface AlertsDAO {

    @SqlQuery("SELECT * FROM alerts where account_id= :account_id and created_at >= :ts")
    List<Alert> since(@Bind("account_id") Long accountId, @Bind("ts") DateTime ts);

    @SingleValueResult(Alert.class)
    @SqlQuery("SELECT * FROM alerts where account_id= :account_id and seen=false order by id desc limit 1;")
    Optional<Alert> mostRecentNotSeen(@Bind("account_id") Long accountId);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO alerts (account_id, title, body, created_at) VALUES(:account_id,:title, :body, :created_at);")
    long insert(@BindAlert Alert alert);

    @SqlUpdate("UPDATE alerts set seen=true where id = :id")
    void seen(@Bind("id") Long id);
}
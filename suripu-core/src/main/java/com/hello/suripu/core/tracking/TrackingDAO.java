package com.hello.suripu.core.tracking;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface TrackingDAO {

    @SqlUpdate("INSERT INTO tracking (sense_id, internal_sense_id, account_id, category, created_at) VALUES (:sense_id, :internal_sense_id, :account_id, :category, now());")
    int insert(@Bind("sense_id") String senseId, @Bind("internal_sense_id") Long internalSenseId, @Bind("account_id") Long accountId, @Bind("category") int category);


    @SqlUpdate("INSERT INTO tracking_out (sense_id, internal_sense_id, account_id, category, created_at, uptime) VALUES (:sense_id, :internal_sense_id, :account_id, :category, now(), :uptime);")
    int uptime(@Bind("sense_id") String senseId, @Bind("internal_sense_id") Long internalSenseId, @Bind("account_id") Long accountId, @Bind("category") Integer category, @Bind("uptime") Integer uptime);
}

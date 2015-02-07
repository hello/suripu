package com.hello.suripu.core.provision;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

@RegisterMapper(PillProvisionMapper.class)
public interface PillProvisionDAO {

    @SqlUpdate("INSERT INTO pill_provision (sn, device_id, created) VALUES(:sn, :device_id, :created);")
    void insert(@Bind("sn") final String serialNumber, @Bind("device_id") final String deviceId, @Bind("created") final DateTime created);

    @SingleValueResult
    @SqlQuery("SELECT * FROM pill_provision WHERE sn = :sn LIMIT 1;")
    Optional<PillProvision> getBySN(@Bind("sn") final String serialNumber);
}

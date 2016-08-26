package com.hello.suripu.core.sense.metadata.sql;

import com.hello.suripu.core.sense.metadata.SenseMetadata;
import com.hello.suripu.core.sense.metadata.SenseMetadataDAO;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

public abstract class SenseMetadataSql implements SenseMetadataDAO {

    @RegisterMapper(SenseMetadataMapper.class)
    @SqlQuery("SELECT * FROM sense_metadata WHERE sense_id = :sense_id ORDER BY id desc LIMIT 1;")
    public abstract SenseMetadata get(@Bind("sense_id") String senseId);

    @SqlUpdate("INSERT INTO sense_metadata (sense_id, color, hw_version) VALUES(:sense_id, :color, :hw_version);")
    public abstract Boolean put(@BindSenseMetadata SenseMetadata senseMetadata);
}

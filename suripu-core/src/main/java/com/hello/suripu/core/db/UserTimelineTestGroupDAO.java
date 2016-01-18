package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

/**
 * Created by benjo on 1/13/16.
 */
public interface UserTimelineTestGroupDAO {
    Optional<Long> getUserGestGroup(final Long accountId,  final DateTime timeToQueryUTC);
    void setUserTestGroup( final Long accountId,  final Long groupId);
}

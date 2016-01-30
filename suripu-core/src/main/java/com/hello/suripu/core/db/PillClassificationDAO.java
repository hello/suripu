package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.binders.BindPillClassification;
import com.hello.suripu.core.db.mappers.PillClassificationMapper;
import com.hello.suripu.core.models.PillClassification;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

/**
 * Created by pangwu on 6/23/15.
 */
public interface PillClassificationDAO {
    @RegisterMapper(PillClassificationMapper.class)
    @SingleValueResult(PillClassification.class)
    @SqlQuery("SELECT * FROM pill_classification WHERE internal_pill_id = :internal_pill_id;")
    Optional<PillClassification> getByInternalPillId(@Bind("internal_pill_id") final long internalPillId);

    @SqlUpdate("UPDATE pill_classification SET max_24hr_diff = 0, max_72hr_diff = 0 WHERE internal_pill_id = :internal_pill_id;")
    Integer resetByInternalPillId(@Bind("internal_pill_id") final long internalPillId);

    @SqlUpdate("INSERT INTO pill_classification (" +
            "internal_pill_id,pill_id,last_24pt_window_ts,last_72pt_window_ts,last_update_batt,max_24hr_diff,max_72hr_diff,class) " +
            "VALUES (:internal_pill_id, :pill_id, :last_24pt_window_ts, :last_72pt_window_ts, :last_update_batt, :max_24hr_diff, :max_72hr_diff, :class);")
    Integer insert(@BindPillClassification final PillClassification classification);

    @SqlUpdate("UPDATE pill_classification SET max_24hr_diff = :max_24hr_diff, max_72hr_diff = :max_72hr_diff, " +
    "last_24pt_window_ts = :last_24pt_window_ts, last_72pt_window_ts = :last_72pt_window_ts, " +
    "last_update_batt = :last_update_batt, class = :class")
    Integer update(@BindPillClassification final PillClassification classification);
}

package com.hello.suripu.research.db;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.mappers.TimelineFeedbackMapper;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TimelineFeedback;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
/**
 * Created by benjo on 6/18/15.
 */

@RegisterMapper(TimelineFeedbackMapper.class)
public abstract class LabelDAOImpl implements LabelDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabelDAOImpl.class);

    @SqlQuery("SELECT DISTINCT account_id FROM timeline_feedback " +
            "WHERE date_of_night >= :start_ts AND date_of_night < :end_ts AND  account_id IN " +
            "(SELECT account_id FROM account_device_map " +
            "JOIN " +
            "(SELECT device_name,COUNT(*) FROM account_device_map GROUP BY device_name HAVING COUNT(*) > 1) as partner_devices " +
            "on account_device_map.device_name = partner_devices.device_name)")
    public abstract List<Long> getPartnerAccountsThatHadFeedback(@Bind("start_ts") final DateTime startDate, @Bind("end_ts") final DateTime endDate);


    @SqlQuery("SELECT * FROM timeline_feedback " +
            "WHERE date_of_night >= :start_ts AND date_of_night < :end_ts AND event_type = :event_type AND account_id IN " +
            "(SELECT account_id FROM account_device_map " +
            "JOIN " +
            "(SELECT device_name,COUNT(*) FROM account_device_map GROUP BY device_name HAVING COUNT(*) > 1) as partner_devices " +
            "on account_device_map.device_name = partner_devices.device_name)")
    public abstract List<TimelineFeedback> getPartnerFeedbackByType(@Bind("start_ts") final DateTime startDate, @Bind("end_ts") final DateTime endDate, @Bind("event_type") final Integer eventType);

    public List<TimelineFeedback> getPartnerFeedbackByType(final DateTime startDate, final DateTime endDate, final String eventType) {
        return getPartnerFeedbackByType(startDate, endDate, Event.Type.valueOf(eventType).getValue());
    }

    public List<TimelineFeedback> getPartnerFeedbackByType(final DateTime startDate, final DateTime endDate, final Event.Type eventType) {
        return getPartnerFeedbackByType(startDate, endDate, eventType.getValue());
    }

        @SqlQuery("SELECT * FROM timeline_feedback " +
            "WHERE date_of_night >= :start_ts AND date_of_night < :end_ts AND  account_id IN " +
            "(SELECT account_id FROM account_device_map " +
            "JOIN " +
            "(SELECT device_name,COUNT(*) FROM account_device_map GROUP BY device_name HAVING COUNT(*) > 1) as partner_devices " +
            "on account_device_map.device_name = partner_devices.device_name)")
    public abstract List<TimelineFeedback> getAllPartnerFeedback(@Bind("start_ts") final DateTime startDate, @Bind("end_ts") final DateTime endDate);

}




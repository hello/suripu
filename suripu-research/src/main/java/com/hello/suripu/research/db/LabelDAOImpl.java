package com.hello.suripu.research.db;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
/**
 * Created by benjo on 6/18/15.
 */
public abstract class LabelDAOImpl implements LabelDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabelDAOImpl.class);

    @SqlQuery("SELECT DISTINCT account_id FROM timeline_feedback " +
            "WHERE date_of_night >= :start_ts AND date_of_night < :end_ts AND  account_id IN " +
            "(SELECT account_id FROM account_device_map " +
            "JOIN " +
            "(SELECT device_name,COUNT(*) FROM account_device_map GROUP BY device_name HAVING COUNT(*) > 1) as partner_devices " +
            "on account_device_map.device_name = partner_devices.device_name)")
    public abstract List<Long> getPartnerAccountsThatHadFeedback(@Bind("start_ts") final DateTime startDate, @Bind("end_ts") final DateTime endDate);

}




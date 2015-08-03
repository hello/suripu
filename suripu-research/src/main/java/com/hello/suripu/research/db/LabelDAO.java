package com.hello.suripu.research.db;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TimelineFeedback;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;

import java.util.List;

/**
 * Created by benjo on 6/18/15.
 */
public interface LabelDAO {
    public List<Long> getPartnerAccountsThatHadFeedback(final DateTime startDate, final DateTime endDate);
    public List<TimelineFeedback> getAllPartnerFeedback(final DateTime startDate, final DateTime endDate);
    public List<TimelineFeedback> getPartnerFeedbackByType(final DateTime startDate, final DateTime endDate, final Integer eventType);
    public List<TimelineFeedback> getPartnerFeedbackByType(final DateTime startDate, final DateTime endDate, final String eventType);
    public List<TimelineFeedback> getPartnerFeedbackByType(final DateTime startDate, final DateTime endDate, final Event.Type eventType);


}

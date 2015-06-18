package com.hello.suripu.research.db;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by benjo on 6/18/15.
 */
public interface LabelDAO {
    public  List<Long> getPartnerAccountsThatHadFeedback(final DateTime startDate, final DateTime endDate);

}

package com.hello.suripu.core.speech;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by ksg on 8/23/16
 */
public interface SpeechTimelineReadDAO {
    /**
     * get single item
     * @param accountId account
     * @param dateTime  timestamp
     * @return optional SpeechTimeline
     */
    Optional<SpeechTimeline> getItem(final Long accountId, final DateTime dateTime);

    /**
     * get last spoken command
     * @param accountId account
     * @param lookBackMinutes look back this much from now
     * @return optional SpeechTimeline
     */
    Optional<SpeechTimeline> getLatest(final Long accountId, final int lookBackMinutes);

    /**
     * get spoken commands between dates (inclusive for both ends), with limit
     * @param accountId account
     * @param startDate query start date
     * @param endDate query end date
     * @param limit number of results to return
     * @return List of SpeechTimeline
     */
    List<SpeechTimeline> getItemsByDate(final Long accountId, final DateTime startDate, final DateTime endDate, final int limit);
}

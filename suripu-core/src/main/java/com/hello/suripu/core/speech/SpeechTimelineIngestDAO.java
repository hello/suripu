package com.hello.suripu.core.speech;

/**
 * Created by ksg on 8/23/16
 */
public interface SpeechTimelineIngestDAO {
    /**
     * Insert one item to DynamoDB
     * @param speechTimeline item to insert
     * @return insert success or failure
     */
    Boolean putItem(SpeechTimeline speechTimeline);
}

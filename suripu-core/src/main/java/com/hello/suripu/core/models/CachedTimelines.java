package com.hello.suripu.core.models;

import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

/**
 * Created by pangwu on 2/27/15.
 */
public class CachedTimelines {
    public final List<Timeline> timeline;
    public final String version;

    private CachedTimelines(final List<Timeline> timeline, final String version){
        this.timeline = timeline;
        this.version = version;
    }

    public static CachedTimelines create(final List<Timeline> timeline, final String version){
        return new CachedTimelines(timeline, version);
    }

    public static CachedTimelines createEmpty(){
        return new CachedTimelines(Collections.<Timeline>emptyList(), "");
    }

    public boolean isEmpty(){
        return this.timeline.size() == 0 && this.version.isEmpty();
    }

    public boolean shouldInvalidate(final String latestVersion, final DateTime targetDateUTC, final DateTime nowUTC, final int maxBackTrackDays){
        if(nowUTC.minusDays(maxBackTrackDays).isAfter(targetDateUTC)){
            if(this.isEmpty()){
                return true;   // Empty, timeline not yet computed, force re-generate.
            }

            if(this.version.equals(latestVersion)){
                return false;  // same version, up to date
            }

            return true;
        }

        return false;  // data too old, never invalidate, always use old timeline.
    }
}

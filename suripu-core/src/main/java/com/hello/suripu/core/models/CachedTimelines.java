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
    public final Long expiredAtMillis;

    private CachedTimelines(final List<Timeline> timeline, final String version, final Long expiredAtMillis){
        this.timeline = timeline;
        this.version = version;
        this.expiredAtMillis = expiredAtMillis;
    }

    public static CachedTimelines create(final List<Timeline> timeline, final String version, final Long expiredAtMillis){
        return new CachedTimelines(timeline, version, expiredAtMillis);
    }

    public static CachedTimelines createEmpty(){
        return new CachedTimelines(Collections.<Timeline>emptyList(), "", -1L);
    }

    public boolean isEmpty(){
        return this.timeline.size() == 0 && this.version.isEmpty() && this.expiredAtMillis == -1;
    }

    public boolean shouldInvalidate(final String latestVersion, final DateTime targetDateUTC, final DateTime nowUTC, final int maxBackTrackDays){
        if(nowUTC.minusDays(maxBackTrackDays).isAfter(targetDateUTC)){
            if(this.isEmpty()){
                return true;   // Empty, timeline not yet computed, force re-generate.
            }

            if(this.version.equals(latestVersion)){
                if(this.expiredAtMillis != -1 && nowUTC.isAfter(this.expiredAtMillis)){
                    return true;
                }
                return false;  // same version, up to date
            }

            return true;
        }

        return false;  // data too old, never invalidate, always use old timeline.
    }
}

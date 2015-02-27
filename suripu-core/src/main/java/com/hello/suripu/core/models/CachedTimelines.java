package com.hello.suripu.core.models;

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
        return new CachedTimelines(Collections.<Timeline>emptyList(), new String(""));
    }

    public boolean isEmpty(){
        return this.timeline.size() == 0 && this.version.isEmpty();
    }
}

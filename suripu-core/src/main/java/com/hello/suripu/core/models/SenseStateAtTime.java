package com.hello.suripu.core.models;

import com.hello.suripu.api.input.State;
import org.joda.time.DateTime;

/**
 * Created by jakepiccolo on 2/19/16.
 */
public class SenseStateAtTime {
    public final State.SenseState state;
    public final DateTime timestamp;
    public final Boolean muted;
    public final Integer systemVolume;

    public SenseStateAtTime(final State.SenseState state, final DateTime timestamp) {
        this.state = state;
        this.timestamp = timestamp;
        this.muted = state.getVoiceControlEnabled();
        this.systemVolume = state.getVolume();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SenseStateAtTime)) {
            return false;
        }
        final SenseStateAtTime otherState = (SenseStateAtTime) obj;
        return (state.equals(otherState.state) && timestamp.equals(otherState.timestamp));
    }
}

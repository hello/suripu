package com.hello.suripu.core.firmware;

import com.google.common.base.Objects;


public class GroupNameRollout {

    public static float DEFAULT_ROLLOUT_VALUE = 100.0f;
    public final String groupName;
    public final float rolloutPercent;

    private GroupNameRollout(String groupName, float rolloutPercent) {
        this.groupName = groupName;
        this.rolloutPercent = rolloutPercent;
    }


    public static GroupNameRollout create(final String groupName, final float rolloutPercent) {
        return new GroupNameRollout(groupName, rolloutPercent);
    }

    public static GroupNameRollout defaultValue(final String groupName) {
        return new GroupNameRollout(groupName, DEFAULT_ROLLOUT_VALUE);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GroupNameRollout)) {
            return false;
        }

        final GroupNameRollout other = (GroupNameRollout) obj;
        return Objects.equal(groupName, other.groupName) &&
                Objects.equal(rolloutPercent, other.rolloutPercent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupName, rolloutPercent);
    }
}

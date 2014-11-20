package com.librato.rollout;

import java.lang.String;import java.util.List;

/**
 * TODO: Document
 */
public interface RolloutAdapter {
    public boolean userFeatureActive(final String feature, long userId, List<String> userGroups);
    public boolean deviceFeatureActive(final String feature, String deviceId, List<String> userGroups);
}

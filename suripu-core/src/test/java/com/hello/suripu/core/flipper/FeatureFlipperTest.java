package com.hello.suripu.core.flipper;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Feature;
import com.hello.suripu.core.util.FeatureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Created by jnorgan on 6/3/15.
 */

public class FeatureFlipperTest {

    @Test
    public void testFeatureActive() {
        final List<String> devices = Lists.newArrayList("fake-sense1","fake-sense2");
        final List<String> featureGroups = Lists.newArrayList("group1","group2");
        final List<String> allGroups = Lists.newArrayList("all");
        final Feature feature = new Feature("test_feature", devices, featureGroups, 10);
        final Feature allGroupsfeature = new Feature("test_feature", devices, allGroups, 10);
        final String goodDevice = "a51cd929-15e9-483d-a504-e3b28dde4fd5";
        final String badDevice = "b5b776cd-843d-4b10-8db5-e4c75d217beb";
        final List<String> matchDeviceGroups = Lists.newArrayList("group1");
        final List<String> noMatchDeviceGroups = Lists.newArrayList("another-group");
        final List<String> noDeviceGroups = Lists.newArrayList();

        assertThat(DynamoDBAdapter.featureActive(feature, goodDevice, matchDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(feature, badDevice, matchDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(feature, goodDevice, noMatchDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(feature, badDevice, noMatchDeviceGroups), is(false));
        assertThat(DynamoDBAdapter.featureActive(feature, goodDevice, noDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(feature, badDevice, noDeviceGroups), is(false));
        assertThat(DynamoDBAdapter.featureActive(allGroupsfeature, goodDevice, noDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(allGroupsfeature, badDevice, noDeviceGroups), is(true));
    }

    @Test
    public void testPercentage() {
        final List<String> accountIds = Lists.newArrayList();
        while ( accountIds.size() < FeatureUtils.MAX_ROLLOUT_VALUE) {
            final String uuid = UUID.randomUUID().toString();
            final Integer hash = Math.abs(uuid.hashCode()) % FeatureUtils.MAX_ROLLOUT_VALUE;
            if (hash.equals(accountIds.size())) {
                accountIds.add(uuid);
            }
        }
        final Integer percentage = 40;
        final List<String> correctIds = new ArrayList<>();
        for (final String deviceId : accountIds) {
            if (FeatureUtils.entityIdHashInPercentRange(deviceId, 0, percentage)) {
                correctIds.add(deviceId);
            }
        }
        assertThat(correctIds.size(), is(percentage));

        correctIds.clear();
        for (final String deviceId : accountIds) {
            if (FeatureUtils.entityIdHashInPercentRange(deviceId, 30, percentage)) {
                correctIds.add(deviceId);
            }
        }
        assertThat(correctIds.size(), is(percentage - 30));
    }
}


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
        final Feature feature = new Feature("test_feature", devices, featureGroups, 10.0f);
        final Feature allGroupsFeature = new Feature("test_feature", devices, allGroups, 10.0f);
        final String goodDevice = "8d22f09f-e28c-4883-9a44-0b36c0b51fcd";
        final String badDevice = "49e807dd-f848-4970-80df-dcb6cfbd54a7";
        final List<String> matchDeviceGroups = Lists.newArrayList("group1");
        final List<String> noMatchDeviceGroups = Lists.newArrayList("another-group");
        final List<String> noDeviceGroups = Lists.newArrayList();

        assertThat(DynamoDBAdapter.featureActive(feature, goodDevice, matchDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(feature, badDevice, matchDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(feature, goodDevice, noMatchDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(feature, badDevice, noMatchDeviceGroups), is(false));
        assertThat(DynamoDBAdapter.featureActive(feature, goodDevice, noDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(feature, badDevice, noDeviceGroups), is(false));
        assertThat(DynamoDBAdapter.featureActive(allGroupsFeature, goodDevice, noDeviceGroups), is(true));
        assertThat(DynamoDBAdapter.featureActive(allGroupsFeature, badDevice, noDeviceGroups), is(true));
    }

    @Test
    public void testPercentage() {
        final List<String> accountIds = Lists.newArrayList();
        while ( accountIds.size() < FeatureUtils.MAX_ROLLOUT_VALUE) {
            final String uuid = UUID.randomUUID().toString();
            final Float hash = (Math.abs(uuid.hashCode()) % (FeatureUtils.MAX_ROLLOUT_VALUE * 100.00f) / 100.0f);
            if ((accountIds.size() + 1) > hash && hash > accountIds.size()) {
                accountIds.add(uuid);
            }
        }
        final Float percentage = 40.0f;
        final List<String> correctIds = new ArrayList<>();
        for (final String deviceId : accountIds) {
            if (FeatureUtils.entityIdHashInPercentRange(deviceId, 0.0f, percentage)) {
                correctIds.add(deviceId);
            }
        }
        assertThat(correctIds.size(), is(percentage.intValue()));

        correctIds.clear();
        for (final String deviceId : accountIds) {
            if (FeatureUtils.entityIdHashInPercentRange(deviceId, 30.0f, percentage)) {
                correctIds.add(deviceId);
            }
        }
        final Integer subSet = percentage.intValue() - 30;
        assertThat(correctIds.size(), is(subSet));
    }
}


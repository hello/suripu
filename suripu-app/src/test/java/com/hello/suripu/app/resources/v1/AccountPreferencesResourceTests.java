package com.hello.suripu.app.resources.v1;

import com.google.common.collect.Maps;
import com.hello.suripu.core.preferences.PreferenceName;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class AccountPreferencesResourceTests {
    @Test
    public void filterEntries() {
        Map<PreferenceName, Boolean> unfiltered = Maps.newHashMap();
        unfiltered.put(PreferenceName.ENHANCED_AUDIO, true);
        unfiltered.put(PreferenceName.TEMP_CELSIUS, true);
        unfiltered.put(PreferenceName.TIME_TWENTY_FOUR_HOUR, true);
        unfiltered.put(PreferenceName.PUSH_SCORE, true);
        unfiltered.put(PreferenceName.PUSH_ALERT_CONDITIONS, true);
        unfiltered.put(PreferenceName.WEIGHT_METRIC, true);
        unfiltered.put(PreferenceName.HEIGHT_METRIC, true);

        final Map<PreferenceName, Boolean> filtered = AccountPreferencesResource.filterEntries(unfiltered);
        assertThat(filtered.keySet(), hasItems(
                PreferenceName.ENHANCED_AUDIO,
                PreferenceName.TEMP_CELSIUS,
                PreferenceName.TIME_TWENTY_FOUR_HOUR,
                PreferenceName.PUSH_SCORE,
                PreferenceName.PUSH_ALERT_CONDITIONS
        ));
        assertThat(filtered.keySet(), not(hasItems(
                PreferenceName.WEIGHT_METRIC,
                PreferenceName.HEIGHT_METRIC
        )));
    }
}

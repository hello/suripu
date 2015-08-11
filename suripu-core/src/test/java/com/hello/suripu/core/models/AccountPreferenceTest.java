package com.hello.suripu.core.models;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.preferences.AccountPreference;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.preferences.PreferenceName;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccountPreferenceTest {


    @Test
    public void testSerialization() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final AccountPreference pref = new AccountPreference(PreferenceName.TEMP_CELSIUS, Boolean.FALSE);
        final String prefString = mapper.writeValueAsString(pref);

        final AccountPreference preference = mapper.readValue(prefString, AccountPreference.class);
        assertThat(preference.enabled, equalTo(pref.enabled));
        assertThat(preference.key, equalTo(pref.key));
    }

    @Test
    public void testDefaults() {

        final Map<String, AttributeValue> inDB = Maps.newHashMap();
        final Map<PreferenceName, Boolean> accountPrefs = AccountPreferencesDynamoDB.itemToPreferences(inDB, Sets.newHashSet(PreferenceName.PUSH_SCORE));

        assertThat(accountPrefs.get(PreferenceName.ENHANCED_AUDIO), is(Boolean.FALSE));
        assertThat(accountPrefs.get(PreferenceName.PUSH_ALERT_CONDITIONS), is(Boolean.FALSE));
        assertThat(accountPrefs.get(PreferenceName.TEMP_CELSIUS), is(Boolean.FALSE));

        assertThat(accountPrefs.get(PreferenceName.PUSH_SCORE), is(Boolean.TRUE));

    }

    @Test
    public void testDataInDB() {

        final Map<String, AttributeValue> inDB = Maps.newHashMap();
        inDB.put(PreferenceName.ENHANCED_AUDIO.name(), new AttributeValue().withBOOL(Boolean.TRUE));
        inDB.put(PreferenceName.PUSH_SCORE.name(), new AttributeValue().withBOOL(Boolean.FALSE));

        final Map<PreferenceName, Boolean> accountPrefs = AccountPreferencesDynamoDB.itemToPreferences(inDB, Sets.newHashSet(PreferenceName.PUSH_SCORE));

        assertThat(accountPrefs.get(PreferenceName.ENHANCED_AUDIO), is(Boolean.TRUE));
        assertThat(accountPrefs.get(PreferenceName.PUSH_SCORE), is(Boolean.FALSE));

    }
}

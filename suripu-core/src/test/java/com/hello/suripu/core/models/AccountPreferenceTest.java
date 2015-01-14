package com.hello.suripu.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.suripu.core.preferences.AccountPreference;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccountPreferenceTest {


    @Test
    public void testSerialization() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final AccountPreference pref = new AccountPreference(AccountPreference.EnabledPreference.ENHANCED_AUDIO, Boolean.FALSE);
        final String prefString = mapper.writeValueAsString(pref);

        final AccountPreference preference = mapper.readValue(prefString, AccountPreference.class);
        assertThat(preference.enabled, equalTo(pref.enabled));
        assertThat(preference.key, equalTo(pref.key));
    }
}

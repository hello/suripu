package com.hello.suripu.core.preferences;

import java.util.Map;

public interface AccountPreferencesDAO {

    AccountPreference put(Long accountId, AccountPreference preference);
    Map<PreferenceName, Boolean> putAll(Long accountId, Map<PreferenceName, Boolean> changes);
    Map<PreferenceName, Boolean> get(Long accountId);
}

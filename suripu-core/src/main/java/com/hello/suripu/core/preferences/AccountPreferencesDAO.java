package com.hello.suripu.core.preferences;

import java.util.Map;

public interface AccountPreferencesDAO {

    AccountPreference put(Long accountId, AccountPreference preference);
    Map<AccountPreference.EnabledPreference, Boolean> putAll(Long accountId, Map<AccountPreference.EnabledPreference, Boolean> changes);
    Map<AccountPreference.EnabledPreference, Boolean> get(Long accountId);
}

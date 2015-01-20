package com.hello.suripu.core.preferences;

import java.util.Map;

public interface AccountPreferencesDAO {

    AccountPreference put(Long accountId, AccountPreference preference);
    Map<AccountPreference.EnabledPreference, Boolean> get(Long accountId);
}

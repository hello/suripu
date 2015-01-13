package com.hello.suripu.core.preferences;

import java.util.List;

public interface AccountPreferencesDAO {

    AccountPreference put(Long accountId, AccountPreference preference);
    List<AccountPreference> get(Long accountId);
}

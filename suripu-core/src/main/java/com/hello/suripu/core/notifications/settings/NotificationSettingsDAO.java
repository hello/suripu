package com.hello.suripu.core.notifications.settings;

import java.util.List;

public interface NotificationSettingsDAO {

    void save(List<NotificationSetting> settings);
    List<NotificationSetting> get(Long accountId);
}

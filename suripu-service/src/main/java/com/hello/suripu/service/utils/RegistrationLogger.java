package com.hello.suripu.service.utils;

import com.google.common.base.Optional;
import com.hello.suripu.service.registration.PairingStatus;

public interface RegistrationLogger {
    void setSenseId(String senseId);

    void setAccountId(Long accountId);

    void logFailure(Optional<String> pillId,
                    String info);

    void logProgress(Optional<String> pillId,
                     String info);

    void logSuccess(Optional<String> pillId,
                    String info);

    void logPill(PairingStatus pairingStatus, String pillId);
    void logSense(PairingStatus pairingStatus);

    boolean commit();
}

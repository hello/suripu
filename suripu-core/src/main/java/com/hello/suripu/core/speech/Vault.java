package com.hello.suripu.core.speech;

import com.google.common.base.Optional;

import java.util.Map;

/**
 * Created by ksg on 8/24/16
 */
public interface Vault {
    Optional<String> encrypt(final String plainText, final Map<String, String> encryptionContext);

    Optional<String> decrypt(final String cipherText, final Map<String, String> encryptionContext);
}

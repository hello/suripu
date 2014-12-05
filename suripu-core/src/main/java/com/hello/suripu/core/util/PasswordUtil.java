package com.hello.suripu.core.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String encrypt(final String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
}

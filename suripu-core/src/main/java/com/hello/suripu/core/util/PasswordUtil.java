package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableSet;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Set;

public class PasswordUtil {

    public static String encrypt(final String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static Boolean isNotSecure(final String password) {
        return commonPasswords.contains(password);
    }

    private final static Set<String> commonPasswords;
    static {
        commonPasswords = ImmutableSet.of(
                "password",
                "123456",
                "12345678",
                "1234",
                "qwerty",
                "12345",
                "dragon",
                "pussy",
                "baseball",
                "football",
                "letmein",
                "monkey",
                "696969",
                "abc123",
                "mustang",
                "michael",
                "shadow",
                "master",
                "jennifer",
                "111111",
                "2000",
                "jordan",
                "superman",
                "harley",
                "1234567",
                "fuckme",
                "hunter",
                "fuckyou",
                "trustno1",
                "ranger",
                "buster",
                "thomas",
                "tigger",
                "robert",
                "soccer",
                "fuck",
                "batm",
                "test",
                "pass",
                "killer",
                "hockey",
                "george",
                "charlie",
                "andrew",
                "michelle",
                "love",
                "sunshine",
                "jessica",
                "asshole",
                "6969",
                "pepper",
                "daniel",
                "access",
                "123456789",
                "654321",
                "joshua",
                "maggie",
                "starwars",
                "silver",
                "william",
                "dallas",
                "yankees",
                "123123",
                "ashley",
                "666666",
                "hello",
                "amanda",
                "orange",
                "biteme",
                "freedom",
                "computer",
                "sexy",
                "thunder",
                "nicole",
                "ginger",
                "heather",
                "hammer",
                "summer",
                "corvette",
                "taylor"
        );
    }
}

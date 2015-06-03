package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
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
                "qwerty",
                "dragon",
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
                "batman",
                "killer",
                "hockey",
                "george",
                "charlie",
                "andrew",
                "michelle",
                "sunshine",
                "jessica",
                "asshole",
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
                "amanda",
                "orange",
                "biteme",
                "freedom",
                "computer",
                "thunder",
                "nicole",
                "ginger",
                "heather",
                "hammer",
                "summer",
                "corvette",
                "taylor",
                "fucker",
                "austin",
                "merlin",
                "matthew",
                "121212",
                "golfer",
                "cheese",
                "princess",
                "martin",
                "chelsea",
                "patrick",
                "richard",
                "diamond",
                "yellow",
                "bigdog",
                "secret",
                "asdfgh",
                "sparky",
                "cowboy",
                "camaro",
                "anthony",
                "matrix",
                "falcon",
                "iloveyou",
                "bailey",
                "guitar",
                "jackson",
                "purple",
                "scooter",
                "phoenix",
                "aaaaaa"
        );
            // https://raw.githubusercontent.com/discourse/discourse/master/lib/common_passwords/10k-common-passwords.txt
            // Top 100 with password length >= 6
    }

    public static String obfuscate(@NotNull final String password) {
        if(password == null || password.length() < 2) {
            return "?";
        }
        return password.substring(0, 1) + "...";
    }
}

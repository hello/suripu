package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.util.PasswordUtil;

public class PasswordUpdate {

    public final String currentPassword;
    public final String newPassword;

    @JsonCreator
    public PasswordUpdate(
            @JsonProperty("current_password") final String currentPassword,
            @JsonProperty("new_password") final String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }


    public static PasswordUpdate encrypt(final PasswordUpdate passwordUpdate) {
        return new PasswordUpdate(passwordUpdate.currentPassword, PasswordUtil.encrypt(passwordUpdate.newPassword));
    }
}

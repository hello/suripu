package com.hello.suripu.core.passwordreset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.core.util.PasswordUtil;

import java.util.UUID;

public class UpdatePasswordRequest {
    public final String password;
    public final UUID uuid;
    public final String state;

    private UpdatePasswordRequest(final String password, final UUID uuid, final String state) {
        this.password = password;
        this.uuid = uuid;
        this.state = state;
    }

    @JsonCreator
    public static UpdatePasswordRequest create(
            @JsonProperty("password") final String password,
            @JsonProperty("uuid") final String uuid,
            @JsonProperty("state") final String state) {

        return new UpdatePasswordRequest(password, UUID.fromString(uuid), state);
    }


    public static Optional<UpdatePasswordRequest> encrypt(UpdatePasswordRequest updatePasswordRequest) {
        if(PasswordUtil.isNotSecure(updatePasswordRequest.password) || updatePasswordRequest.password.length() < 6) {
            return Optional.absent();
        }

        return Optional.of(new UpdatePasswordRequest(PasswordUtil.encrypt(updatePasswordRequest.password), updatePasswordRequest.uuid, updatePasswordRequest.state));
    }
}

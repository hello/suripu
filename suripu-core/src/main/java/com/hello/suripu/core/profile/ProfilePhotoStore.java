package com.hello.suripu.core.profile;

import com.google.common.base.Optional;

public interface ProfilePhotoStore {

    Optional<ImmutableProfilePhoto> get(Long accountId);
    boolean put(ProfilePhoto profilePhoto);
    void delete(Long accountId);
}

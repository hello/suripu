package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AccessTokenTest {

    @Test
    public void testAccessTokenFormat()  {

        final String invalidAccessTokenString = "abcd";
        assertThat(false, is(AccessToken.cleanUUID(invalidAccessTokenString).isPresent()));

        final String validAccessTokenString = "3.ebf604cff01544c08b5c5ba629222773";
        final Optional<UUID> optionalUUID = AccessToken.cleanUUID(validAccessTokenString);

        assertThat(true, is(optionalUUID.isPresent()));
        assertThat(UUID.fromString("ebf604cf-f015-44c0-8b5c-5ba629222773"), is(optionalUUID.get()));
    }
}

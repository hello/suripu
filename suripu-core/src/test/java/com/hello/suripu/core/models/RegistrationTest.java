package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RegistrationTest {

    @Test
    public void testValidEmail() {
        final Optional<Registration.RegistrationError> error = Registration.validateEmail("attack@sneak.berlin");
        assertThat(error.isPresent(), is(false));
    }

    @Test
    public void testInvalidEmail() {
        final Optional<Registration.RegistrationError> error = Registration.validateEmail("nobody@noplace.somedog");
        assertThat(error.isPresent(), is(true));
    }
}

package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RegistrationTest {

    private static class NameCombo {
        public final String name;
        public final String firstname;
        public final String lastname;

        private NameCombo(final String name, final String firstname, final String lastname) {
            this.name = name;
            this.firstname = firstname;
            this.lastname = lastname;
        }

        public static NameCombo with(final String name, final String firstname, final String lastname) {
            return new NameCombo(name, firstname, lastname);
        }
    }
    private Registration newRegistrationWithName(final String name, final String firstname, final String lastname) {
        return new Registration(name,  firstname, lastname, "email@email.com", "password", 123,
                Gender.OTHER, "", 123, 321, DateTime.now(), 10, 0.0, 0.0);
    }

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

    @Test
    public void validateNameCombo() {

        final NameCombo[] combos = new NameCombo[]{
                NameCombo.with(null, null, null),
                NameCombo.with(null, null, ""),
                NameCombo.with(null, "", ""),
                NameCombo.with("", "", ""),
                NameCombo.with("", "", null),
                NameCombo.with("", null, null),

        };

        for(final NameCombo combo : combos) {
            final Registration registration = newRegistrationWithName(combo.name, combo.firstname, combo.lastname);
            final Optional<Registration.RegistrationError> error = Registration.validate(registration);
            MatcherAssert.assertThat(error.isPresent(), is(true));
        }


    }
}

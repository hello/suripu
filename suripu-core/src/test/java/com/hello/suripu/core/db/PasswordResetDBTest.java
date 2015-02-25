package com.hello.suripu.core.db;

import com.hello.suripu.core.passwordreset.PasswordResetDB;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PasswordResetDBTest {

    @Test
    public void checkHasExpired() {
        final DateTime createdAt = new DateTime(2015,02,02, 0,0,0,DateTimeZone.UTC);
        final DateTime now = createdAt.plusHours(12);
        final Boolean hasExpired = PasswordResetDB.hasExpired(createdAt.getMillis(), now, 24);
        assertThat(hasExpired, is(false));
    }
}

package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 10/3/16.
 */
public class AccountUtilsTest {

    @Test
    public void test_noDob() {
        final Account account = new Account.Builder()
                .withCreated(DateTime.now())
                .withDOB(DateTime.now())
                .build();

        final Optional<Account> accountOptional = Optional.of(account);

        final Integer age = AccountUtils.getUserAgeYears(accountOptional);
        assertThat(age, is(AccountUtils.ADULT_AGE_YEARS));
    }

    @Test
    public void test_noDob2() {
        final Optional<Account> accountOptional = Optional.absent();

        final Integer age = AccountUtils.getUserAgeYears(accountOptional);
        assertThat(age, is(AccountUtils.ADULT_AGE_YEARS));
    }

    @Test
    public void test_dob() {
        final Account account = new Account.Builder()
                .withCreated(DateTime.now())
                .withDOB(DateTime.now().minusYears(20))
                .build();

        final Optional<Account> accountOptional = Optional.of(account);

        final Integer age = AccountUtils.getUserAgeYears(accountOptional);
        assertThat(age, is(20));
    }

}

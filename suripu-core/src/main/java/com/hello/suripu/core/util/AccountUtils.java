package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Years;

/**
 * Created by jyfan on 10/3/16.
 */
public class AccountUtils {

    public static final Integer ADULT_AGE_YEARS = 35;
    public static final DateTime EARLIEST_ALLOWABLE_DOB = new DateTime(1990, 1, 1, 0, 0, 0);

    public static Integer getUserAgeYears(Optional<Account> account) {
        if (!account.isPresent()) {
            return ADULT_AGE_YEARS;
        }

        if (account.get().DOB.withTimeAtStartOfDay().isEqual(account.get().created.withTimeAtStartOfDay())) {//DOB is stored as created date (hour,min,sec removed) if user does not input DOB
            return ADULT_AGE_YEARS;
        }

        if (account.get().DOB.withTimeAtStartOfDay().isBefore(EARLIEST_ALLOWABLE_DOB)) {
            return ADULT_AGE_YEARS;
        }

        final DateTime dob = account.get().DOB;
        final Integer userAge = Years.yearsBetween(dob, DateTime.now(DateTimeZone.UTC)).toPeriod().getYears();
        return userAge;

    }
}

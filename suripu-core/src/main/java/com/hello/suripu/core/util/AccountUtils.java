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

    public static Integer getUserAgeYears(Optional<Account> account) {
        if (account.isPresent()) {
            if (!account.get().DOB.isEqual(account.get().created)) { //DOB is stored as created date if user does not input DOB
                final DateTime dob = account.get().DOB;
                final Integer userAge = Years.yearsBetween(dob, DateTime.now(DateTimeZone.UTC)).toPeriod().getYears();
                return userAge;
            }
        }

        return ADULT_AGE_YEARS;
    }
}

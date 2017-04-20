package com.hello.suripu.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Created by jarredheinrich on 12/22/16.
 */
public class UserBioInfoTest {

    final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    public void getUserBioInfoPresentTest()throws IOException {

        Account accountTest =  new Account.Builder()
                .withId(0L)
                .build();
        UserBioInfo userBioInfo = UserBioInfo.getUserBioInfo(Optional.of(accountTest),false,  false);
        assertThat(userBioInfo.age, is(0.0) );
        assertThat(userBioInfo.bmi, is(0.0));
        assertThat(userBioInfo.male, is(0));
        assertThat(userBioInfo.female, is(0));
        assertThat(userBioInfo.partner, is(0));

        final double age = 20.0;
        final int height = 160;
        final int weight = 70000;
        final double bmi = (weight/ 1000)/( (((double)height) / 100) * (((double)height)  / 100));
        final Gender gender = Gender.MALE;
        final DateTime dob = DateTime.now(DateTimeZone.UTC).minusYears((int) age);
        accountTest =  new Account.Builder()
                .withId(0L)
                .withDOB(dob.toString())
                .withGender(gender)
                .withHeight(height)
                .withWeight(weight)
                .build();
        userBioInfo = UserBioInfo.getUserBioInfo(Optional.of(accountTest), false, true);
        assertThat(userBioInfo.age, is(age));
        assertThat(userBioInfo.bmi, is(bmi));
        assertThat(userBioInfo.male, is(1));
        assertThat(userBioInfo.female, is(0));
        assertThat(userBioInfo.partner, is(1));

    }

    @Test
    public void getUserBioInfoAbsentTest(){
        final Optional<Account> accountOptional = Optional.absent();
        final UserBioInfo userBioInfo = UserBioInfo.getUserBioInfo(accountOptional,false,  false);
        assertThat(userBioInfo.age, is(0.0));
        assertThat(userBioInfo.bmi, is(0.0));
        assertThat(userBioInfo.male, is(0));
        assertThat(userBioInfo.female, is(0));
        assertThat(userBioInfo.partner, is(0));

    }
}

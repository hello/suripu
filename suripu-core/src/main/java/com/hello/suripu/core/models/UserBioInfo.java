package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.util.DateTimeUtil;

/**
 * Created by jarredheinrich on 12/22/16.
 */
public class UserBioInfo {
    public final double age;
    public final double bmi;
    public final int male;
    public final int female;
    public final int partner;
    public final SleepPeriod.Period primarySleepPeriod;

    private final static int BMI_MIN = 5;
    private final static int BMI_MAX = 50;
    private final static double BMI_DEFAULT = 0.0;
    private final static int AGE_MIN = 18;
    private final static int AGE_MAX = 90;
    private final static double AGE_DEFAULT = 0.0;

    public UserBioInfo( final double age, final double bmi, final int male, final int female, final int partner, final SleepPeriod.Period sleepPeriod){
        this.age = age;
        this.bmi = bmi;
        this.male = male;
        this.female = female;
        this.partner = partner;
        this.primarySleepPeriod = sleepPeriod;
    }

    public UserBioInfo( final double age, final double bmi, final int male, final int female, final int partner){
        this.age = age;
        this.bmi = bmi;
        this.male = male;
        this.female = female;
        this.partner = partner;
        this.primarySleepPeriod = SleepPeriod.Period.NIGHT;
    }

    public UserBioInfo() {
        this.age = AGE_DEFAULT;
        this.bmi = BMI_DEFAULT;
        this.male = 0;
        this.female = 0;
        this.partner = 0;
        this.primarySleepPeriod =  SleepPeriod.Period.NIGHT;
    }

    public static UserBioInfo forDaySleeper(Optional<Account> accountOptional, final boolean hasPartnerPresent){
        return getUserBioInfo(accountOptional, true, hasPartnerPresent);
    }

    public static UserBioInfo forNightSleeper(Optional<Account> accountOptional, final boolean hasPartnerPresent){
        return getUserBioInfo(accountOptional, false, hasPartnerPresent);
    }

    public static UserBioInfo getUserBioInfo(Optional<Account> accountOptional, final boolean daySleeper, final boolean hasPartnerPresent) {
        final int male, female, partner;
        final double age, bmi;
        if (accountOptional.isPresent()) {
            final double userAge = DateTimeUtil.getDateDiffFromNowInDays(accountOptional.get().DOB) / 365;
            final double height = ((double) accountOptional.get().height) / 100;
            final double weight = ((double) accountOptional.get().weight) / 1000;
            //age
            if (userAge >= AGE_MIN && userAge <= AGE_MAX) {
                age = userAge;
            } else {
                age = AGE_DEFAULT;
            }
            //bmi
            if (height > 0 && weight > 0) {
                final double userBMI = weight / (height * height);
                if (userBMI > BMI_MIN && userBMI < BMI_MAX) {
                    bmi = userBMI;
                } else {
                    bmi = BMI_DEFAULT;
                }
            } else {
                bmi = BMI_DEFAULT;
            }
            //gender
            final Gender gender = accountOptional.get().gender;
            if (gender == Gender.MALE) {
                male = 1;
                female = 0;
            } else if (gender == Gender.FEMALE) {
                male = 0;
                female = 1;
            } else {
                male = 0;
                female = 0;
            }
        } else {
            age = AGE_DEFAULT;
            bmi = BMI_DEFAULT;
            male = 0;
            female = 0;
        }
        //partner
        if (hasPartnerPresent) {
            partner = 1;
        } else {
            partner = 0;
        }
        final SleepPeriod.Period sleepPeriod;
        if (daySleeper){
            sleepPeriod = SleepPeriod.Period.MORNING;
        } else {
            sleepPeriod = SleepPeriod.Period.NIGHT;
        }

        return new UserBioInfo(age, bmi, male, female, partner, sleepPeriod);
    }

}

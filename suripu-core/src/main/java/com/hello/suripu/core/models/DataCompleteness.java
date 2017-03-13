package com.hello.suripu.core.models;

import java.util.List;

/**
 * Created by jakepiccolo on 12/14/15.
 */
public enum DataCompleteness {
    NO_DATA(0),
    NOT_ENOUGH_DATA(1),
    ENOUGH_DATA(2);
    private int value;

    private DataCompleteness(int value){
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }

    public static DataCompleteness getAllPeriodsDataCompleteness(final List<DataCompleteness> dataCompletenessList, final int numSleepPeriods){
        DataCompleteness overallDataCompleteness = NO_DATA;
        for (final DataCompleteness dataCompleteness : dataCompletenessList){
            if(dataCompleteness.getValue() > overallDataCompleteness.getValue()){
                overallDataCompleteness = dataCompleteness;
            }
        }
        if (numSleepPeriods < 3 && overallDataCompleteness == ENOUGH_DATA){
            overallDataCompleteness = NOT_ENOUGH_DATA;
        }
        return overallDataCompleteness;
    }

}

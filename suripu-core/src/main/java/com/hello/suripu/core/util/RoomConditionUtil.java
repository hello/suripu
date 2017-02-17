package com.hello.suripu.core.util;


import com.hello.suripu.core.roomstate.Condition;
import com.hello.suripu.core.roomstate.CurrentRoomState;
import com.hello.suripu.core.roomstate.State;

/**
 * Created by pangwu on 11/18/14.
 */
public class RoomConditionUtil {
    private static final int FULL_SCORE = 100;
    private static final int WARNING_SCORE = 60;
    private static final int BAD_SCORE = 10;

    private static final int WARNING_PERCENTAGE = 75;
    private static final int BAD_PERCENTAGE = 30;

    public static Condition getGeneralRoomCondition(final CurrentRoomState currentRoomState) {
        float numberOfModality = 3;
        float totalScore = numberOfModality * 100;
        float currentScore = 0;

        currentScore += FULL_SCORE * getIdealCountFromSate(currentRoomState.humidity());
        currentScore += FULL_SCORE * getIdealCountFromSate(currentRoomState.dust());
        currentScore += FULL_SCORE * getIdealCountFromSate(currentRoomState.temperature());

        currentScore += WARNING_SCORE * getWarningCountFromSate(currentRoomState.humidity());
        currentScore += WARNING_SCORE * getWarningCountFromSate(currentRoomState.dust());
        currentScore += WARNING_SCORE * getWarningCountFromSate(currentRoomState.temperature());

        currentScore += BAD_SCORE * getAlertCountFromSate(currentRoomState.humidity());
        currentScore += BAD_SCORE * getAlertCountFromSate(currentRoomState.dust());
        currentScore += BAD_SCORE * getAlertCountFromSate(currentRoomState.temperature());

        float percentage = currentScore / totalScore * 100;
        if(percentage > WARNING_PERCENTAGE){
            return Condition.IDEAL;
        }else if(percentage > BAD_PERCENTAGE){
            return Condition.WARNING;
        }else{
            return Condition.ALERT;
        }

    }

    public static Condition getRoomConditionV2LightOff(final CurrentRoomState currentRoomState, final Boolean hasCalibration){
        final CurrentRoomState currentRoomStateWithoutLight = new CurrentRoomState(
                currentRoomState.temperature(),
                currentRoomState.humidity(),
                currentRoomState.dust(),
                new State(currentRoomState.light().value,
                        currentRoomState.light().message,
                        currentRoomState.light().idealConditions,
                        Condition.IDEAL,
                        currentRoomState.light().lastUpdated,
                        currentRoomState.light().unit),
                currentRoomState.sound(), hasCalibration);
        return getGeneralRoomConditionV2(currentRoomStateWithoutLight, hasCalibration);
    }

    public static Condition getGeneralRoomConditionV2(final CurrentRoomState currentRoomState, final Boolean hasCalibration) {
        float warningCount = 0;
        float alertCount = 0;

        //TODO: Add particulate values back into room condition after it becomes visible to the user
        warningCount += getWarningCountFromSate(currentRoomState.humidity());
        warningCount += getWarningCountFromSate(currentRoomState.temperature());
        warningCount += getWarningCountFromSate(currentRoomState.light());
        warningCount += getWarningCountFromSate(currentRoomState.sound());
        if (hasCalibration) {
            warningCount += getWarningCountFromSate(currentRoomState.dust());
        }

        alertCount += getAlertCountFromSate(currentRoomState.humidity());
        alertCount += getAlertCountFromSate(currentRoomState.temperature());
        alertCount += getAlertCountFromSate(currentRoomState.light());
        alertCount += getAlertCountFromSate(currentRoomState.sound());
        if (hasCalibration) {
            alertCount += getAlertCountFromSate(currentRoomState.dust());
        }


        if(alertCount > 0) {
            return Condition.ALERT;
        }

        if(warningCount > 0) {
            return Condition.WARNING;
        }

        return Condition.IDEAL;
    }

    private static int getIdealCountFromSate(State state) {
        if(state.condition() == Condition.IDEAL){
            return 1;
        }
        return 0;
    }

    private static int getAlertCountFromSate(State state) {
        if(state.condition() == Condition.ALERT){
            return 1;
        }
        return 0;
    }

    private static int getWarningCountFromSate(State state) {
        if(state.condition() == Condition.WARNING){
            return 1;
        }
        return 0;
    }
}

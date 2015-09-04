package com.hello.suripu.core.util;


import com.hello.suripu.core.models.CurrentRoomState;

/**
 * Created by pangwu on 11/18/14.
 */
public class RoomConditionUtil {
    private static final int FULL_SCORE = 100;
    private static final int WARNING_SCORE = 60;
    private static final int BAD_SCORE = 10;

    private static final int WARNING_PERCENTAGE = 75;
    private static final int BAD_PERCENTAGE = 30;

    public static CurrentRoomState.State.Condition getGeneralRoomCondition(final CurrentRoomState currentRoomState) {
        float numberOfModality = 3;
        float totalScore = numberOfModality * 100;
        float currentScore = 0;

        currentScore += FULL_SCORE * getIdealCountFromSate(currentRoomState.humidity);
        currentScore += FULL_SCORE * getIdealCountFromSate(currentRoomState.particulates);
        currentScore += FULL_SCORE * getIdealCountFromSate(currentRoomState.temperature);

        currentScore += WARNING_SCORE * getWarningCountFromSate(currentRoomState.humidity);
        currentScore += WARNING_SCORE * getWarningCountFromSate(currentRoomState.particulates);
        currentScore += WARNING_SCORE * getWarningCountFromSate(currentRoomState.temperature);

        currentScore += BAD_SCORE * getAlertCountFromSate(currentRoomState.humidity);
        currentScore += BAD_SCORE * getAlertCountFromSate(currentRoomState.particulates);
        currentScore += BAD_SCORE * getAlertCountFromSate(currentRoomState.temperature);

        float percentage = currentScore / totalScore * 100;
        if(percentage > WARNING_PERCENTAGE){
            return CurrentRoomState.State.Condition.IDEAL;
        }else if(percentage > BAD_PERCENTAGE){
            return CurrentRoomState.State.Condition.WARNING;
        }else{
            return CurrentRoomState.State.Condition.ALERT;
        }

    }

    public static CurrentRoomState.State.Condition getRoomConditionV2LightOff(final CurrentRoomState currentRoomState, final Boolean hasCalibration){
        final CurrentRoomState currentRoomStateWithoutLight = new CurrentRoomState(currentRoomState.temperature,
                currentRoomState.humidity,
                currentRoomState.particulates,
                new CurrentRoomState.State(currentRoomState.light.value,
                        currentRoomState.light.message,
                        currentRoomState.light.idealConditions,
                        CurrentRoomState.State.Condition.IDEAL,
                        currentRoomState.light.lastUpdated,
                        currentRoomState.light.unit),
                currentRoomState.sound);
        return getGeneralRoomConditionV2(currentRoomStateWithoutLight, hasCalibration);
    }

    public static CurrentRoomState.State.Condition getGeneralRoomConditionV2(final CurrentRoomState currentRoomState, final Boolean hasCalibration) {
        float warningCount = 0;
        float alertCount = 0;

        //TODO: Add particulate values back into room condition after it becomes visible to the user
        warningCount += getWarningCountFromSate(currentRoomState.humidity);
        warningCount += getWarningCountFromSate(currentRoomState.temperature);
        warningCount += getWarningCountFromSate(currentRoomState.light);
        warningCount += getWarningCountFromSate(currentRoomState.sound);
        if (hasCalibration) {
            warningCount += getWarningCountFromSate(currentRoomState.particulates);
        }

        alertCount += getAlertCountFromSate(currentRoomState.humidity);
        alertCount += getAlertCountFromSate(currentRoomState.temperature);
        alertCount += getAlertCountFromSate(currentRoomState.light);
        alertCount += getAlertCountFromSate(currentRoomState.sound);
        if (hasCalibration) {
            alertCount += getAlertCountFromSate(currentRoomState.particulates);
        }


        if(alertCount > 0) {
            return CurrentRoomState.State.Condition.ALERT;
        }

        if(warningCount > 0) {
            return CurrentRoomState.State.Condition.WARNING;
        }

        return CurrentRoomState.State.Condition.IDEAL;
    }

    private static int getIdealCountFromSate(CurrentRoomState.State state) {
        if(state.condition == CurrentRoomState.State.Condition.IDEAL){
            return 1;
        }
        return 0;
    }

    private static int getAlertCountFromSate(CurrentRoomState.State state) {
        if(state.condition == CurrentRoomState.State.Condition.ALERT){
            return 1;
        }
        return 0;
    }

    private static int getWarningCountFromSate(CurrentRoomState.State state) {
        if(state.condition == CurrentRoomState.State.Condition.WARNING){
            return 1;
        }
        return 0;
    }
}

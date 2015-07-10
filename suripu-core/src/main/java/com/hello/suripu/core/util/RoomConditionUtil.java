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


    public static CurrentRoomState.State.Condition getRoomConditionV2LightOff(final CurrentRoomState currentRoomState){
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
        return getGeneralRoomConditionV2(currentRoomStateWithoutLight);
    }


    public static CurrentRoomState.State.Condition getGeneralRoomConditionV2(final CurrentRoomState currentRoomState) {
        float numberOfModality = 5;
        float warningCount = 0;
        float idealCount = 0;
        float alertCount = 0;

        idealCount += getIdealCountFromSate(currentRoomState.humidity);
        idealCount += getIdealCountFromSate(currentRoomState.particulates);
        idealCount += getIdealCountFromSate(currentRoomState.temperature);
        idealCount += getIdealCountFromSate(currentRoomState.light);
        idealCount += getIdealCountFromSate(currentRoomState.sound);

        warningCount += getWarningCountFromSate(currentRoomState.humidity);
        warningCount += getWarningCountFromSate(currentRoomState.particulates);
        warningCount += getWarningCountFromSate(currentRoomState.temperature);
        warningCount += getWarningCountFromSate(currentRoomState.light);
        warningCount += getWarningCountFromSate(currentRoomState.sound);

        alertCount += getAlertCountFromSate(currentRoomState.humidity);
        alertCount += getAlertCountFromSate(currentRoomState.particulates);
        alertCount += getAlertCountFromSate(currentRoomState.temperature);
        alertCount += getAlertCountFromSate(currentRoomState.light);
        alertCount += getAlertCountFromSate(currentRoomState.sound);


        // decision tree :)
        //
        //  Left child is true, right child is false
        //
        //          bad_count > 1 ?
        //            /         \
        //         alert    ideal_count > 50% ?
        //                      /           \
        //              warn_count > 1 ?    alert
        //                 /       \
        //              warning   ideal
        //

        if(alertCount <= 1){
            if(idealCount / numberOfModality > 0.5f) {
                if(warningCount > 1) {
                    return CurrentRoomState.State.Condition.WARNING;
                }else{
                    return CurrentRoomState.State.Condition.IDEAL;
                }
            }else{
                return CurrentRoomState.State.Condition.ALERT;
            }
        }else{
            return CurrentRoomState.State.Condition.ALERT;
        }
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

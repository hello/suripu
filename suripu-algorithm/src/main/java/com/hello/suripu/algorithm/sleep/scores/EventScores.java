package com.hello.suripu.algorithm.sleep.scores;

/**
 * Created by pangwu on 12/16/14.
 */
public class EventScores {
    public final double goToBedEventScore;
    public final double outOfBedEventScore;

    public final double sleepEventScore;
    public final double wakeUpEventScore;

    public EventScores(final double sleepEventScore, final double wakeUpEventScore,
                       final double goToBedEventScore, final double outOfBedEventScore){
        this.sleepEventScore = sleepEventScore;
        this.wakeUpEventScore = wakeUpEventScore;
        this.goToBedEventScore = goToBedEventScore;
        this.outOfBedEventScore = outOfBedEventScore;
    }

}

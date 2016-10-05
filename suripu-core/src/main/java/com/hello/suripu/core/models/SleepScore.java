package com.hello.suripu.core.models;

/**
 * Created by jimmy on 7/21/15.
 */
public class SleepScore {

    public static final int MAX_SCORE = 100;
    public static final int MIN_SCORE = 15;
    public static final int IDEAL_SCORE_THRESHOLD = 80;
    public static final int WARNING_SCORE_THRESHOLD = 60;
    public static final int ALERT_SCORE_THRESHOLD = 0;
    public static final int NO_SCORE = 0;


    public final MotionScore motionScore;
    public final Integer sleepDurationScore;
    public final Integer environmentalScore;
    public final Integer timesAwakePenaltyScore; // negative
    public final Integer value;
    public final String version;

    public SleepScore(final Integer value,
                      final MotionScore motionScore,
                      final Integer sleepDurationScore,
                      final Integer environmentalScore,
                      final Integer timesAwakePenaltyScore,
                      final String version) {

        this.value = value;
        this.motionScore = motionScore;
        this.sleepDurationScore = sleepDurationScore;
        this.environmentalScore = environmentalScore;
        this.timesAwakePenaltyScore = timesAwakePenaltyScore;
        this.version = version;
    }

    @Override
    public String toString(){
        final StringBuilder builder = new StringBuilder()
                .append("{motion ")
                .append(this.motionScore.score)
                .append(", duration ")
                .append(this.sleepDurationScore)
                .append(", environment ")
                .append(this.environmentalScore)
                .append(", times-awakes-penalty ")
                .append(this.timesAwakePenaltyScore)
                .append(", sleep score ")
                .append(this.value)
                .append(", version ")
                .append(this.version)
                .append("}");
        return builder.toString();
    }

    public static class Weighting {
        public float motion;
        public float duration;
        public float environmental;

        public Weighting() {
            this.motion = 0.7f;
            this.duration = 0.2f;
            this.environmental = 0.1f;
        }
    }

    public static class DurationHeavyWeighting extends Weighting {

        public DurationHeavyWeighting() {
            this.motion = 0.5f;
            this.duration = 0.4f;
        }
    }

    public static class DurationHeavyWeightingV2 extends Weighting {

        public DurationHeavyWeightingV2() {
            this.motion = 0.3f;
            this.duration = 0.6f;
        }
    }

    public static class DurationWeightingV4 extends Weighting {

        public DurationWeightingV4() {
            this.motion = 0.0f;
            this.duration = 0.8f;
            this.environmental = 0.2f;
        }
    }

    public static class DurationWeightingV5 extends Weighting {

        public DurationWeightingV5() {
            this.motion = 0.0f;
            this.duration = 0.8f;
            this.environmental = 0.2f;
        }
    }

    /**
     * We want to use a builder for the Sleep Score because the components of the sleep
     * score will likely increase based on https://github.com/hello/bugs/issues/253
     */
    public static class Builder {

        private MotionScore motionScore;
        private Integer sleepDurationScore;
        private Integer environmentalScore;
        private Integer timesAwakePenaltyScore;
        private Weighting weighting;
        private String version = "";


        public Builder() {
            this.weighting = new Weighting(); // default weighting
            this.timesAwakePenaltyScore = 0;
        }

        public Builder withMotionScore(final MotionScore motionScore) {
            this.motionScore = motionScore;
            return this;
        }

        public Builder withSleepDurationScore(final Integer sleepDurationScore) {
            this.sleepDurationScore = sleepDurationScore;
            return this;
        }

        public Builder withEnvironmentalScore(final Integer environmentalScore) {
            this.environmentalScore = environmentalScore;
            return this;
        }

        public Builder withTimesAwakePenaltyScore(final Integer timesAwakePenaltyScore) {
            this.timesAwakePenaltyScore = timesAwakePenaltyScore;
            return this;
        }

        public Builder withWeighting(final Weighting weighting) {
            this.weighting = weighting;
            return this;
        }

        public Builder withVersion(final String version){
            this.version = version;
            return this;
        }

        public SleepScore build() {
            Integer value = Math.round(
                      (weighting.motion * motionScore.score)
                    + (weighting.duration * sleepDurationScore)
                    + (weighting.environmental * environmentalScore))
                    + timesAwakePenaltyScore;

            if (value < MIN_SCORE) {
                value = MIN_SCORE;
            }

            return new SleepScore(value,
                    motionScore,
                    sleepDurationScore,
                    environmentalScore,
                    timesAwakePenaltyScore,
                    version
                    );
        }
    }
}

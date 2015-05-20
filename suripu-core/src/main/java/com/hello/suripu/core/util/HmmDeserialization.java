package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import com.hello.suripu.algorithm.hmm.GammaPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import org.slf4j.Logger;

import javax.lang.model.element.Name;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by benjo on 3/16/15.
 */
public class HmmDeserialization {

    static public ImmutableList<NamedSleepHmmModel> createModelsFromProtobuf(final SleepHmmProtos.SleepHmmModelSet allModels) {

        List<NamedSleepHmmModel> models = new ArrayList<>();

        for (final SleepHmmProtos.SleepHmm hmmModelData : allModels.getModelsList()) {

            final Optional<NamedSleepHmmModel> namedModel = createIndividualModelFromProtobuf(hmmModelData);

            if (!namedModel.isPresent()) {
                continue;
            }

            models.add(namedModel.get());

        }

        return ImmutableList.copyOf(models);
    }

    static private Optional<NamedSleepHmmModel> createIndividualModelFromProtobuf(final SleepHmmProtos.SleepHmm hmmModelData) {

        String source = "no_source";

        if (hmmModelData.hasSource()) {
            source = hmmModelData.getSource();
        }

        String id = "no_id";

        if (hmmModelData.hasUserId()) {
            id = hmmModelData.getUserId();
        }

        String modelName = String.format("random-id-%s",Long.toHexString(Double.doubleToLongBits(Math.random())));

        if (hmmModelData.hasModelName()) {
            modelName = hmmModelData.getModelName();
        }

        /*
         * magical constants
         *
         */
        int numFreeParams = HmmDataConstants.NUM_MODEL_PARAMS;

        if (hmmModelData.hasNumModelParams()) {
            numFreeParams = hmmModelData.getNumModelParams();
        }


        double audioDisturbanceThresoldDB = HmmDataConstants.AUDIO_DISTURBANCE_THRESHOLD_DB;

        if (hmmModelData.hasAudioDisturbanceThresholdDb()) {
            audioDisturbanceThresoldDB = hmmModelData.getAudioDisturbanceThresholdDb();
        }

        double pillMagnitudeDisturbanceThreshold = HmmDataConstants.PILL_MAGNITUDE_DISTURBANCE_THRESHOLD;

        if (hmmModelData.hasPillMagnitudeDisturbanceThresholdLsb()) {
            pillMagnitudeDisturbanceThreshold = hmmModelData.getPillMagnitudeDisturbanceThresholdLsb();
        }

        double naturalLightFilterStartHour = HmmDataConstants.NATURAL_LIGHT_FILTER_START_HOUR;

        if (hmmModelData.hasNaturalLightFilterStartHour()) {
            naturalLightFilterStartHour = hmmModelData.getNaturalLightFilterStartHour();
        }

        double naturalLightFilterStopHour = HmmDataConstants.NATURAL_LIGHT_FILTER_STOP_HOUR;

        if (hmmModelData.hasNaturalLightFilterStopHour()) {
            naturalLightFilterStopHour = hmmModelData.getNaturalLightFilterStopHour();
        }

        int numMinutesInMeasPeriod = HmmDataConstants.NUM_MINUTES_IN_MEAS_PERIOD;

        if (hmmModelData.hasNumMinutesInMeasPeriod()) {
            numMinutesInMeasPeriod = hmmModelData.getNumMinutesInMeasPeriod();
        }

        boolean isUsingIntervalSearch  = HmmDataConstants.DEFAULT_IS_USING_INTERVAL_SEARCH;
        if (hmmModelData.hasEnableIntervalSearch()) {
            isUsingIntervalSearch = hmmModelData.getEnableIntervalSearch();
        }

        double lightPreMultiplier = HmmDataConstants.DEFAULT_LIGHT_PRE_MULTIPLIER;

        if (hmmModelData.hasLightPreMultiplier()) {
            lightPreMultiplier = hmmModelData.getLightPreMultiplier();
        }

        double lightFloorLux = HmmDataConstants.DEFAULT_LIGHT_FLOOR_LUX;

        if (hmmModelData.hasLightFloorLux()) {
            lightFloorLux = hmmModelData.getLightFloorLux();
        }

        boolean useWaveCountsAsDisturbances = HmmDataConstants.DEFAULT_USE_WAVE_COUNTS_FOR_DISTURBANCES;

        if (hmmModelData.hasUseWaveAsDisturbance()) {
            useWaveCountsAsDisturbances = hmmModelData.getUseWaveAsDisturbance();
        }

        double audioLevelAboveBackroundThresholdDb = HmmDataConstants.DEFAULT_AUDIO_ABOVE_BACKGROUND_THRESHOLD_DB;
        if (hmmModelData.hasAudioLevelAboveBackgroundThresholdDb()) {
            audioLevelAboveBackroundThresholdDb = hmmModelData.getAudioLevelAboveBackgroundThresholdDb();
        }
         /*
         * interpretation parameters (which states mean what)
         *
         */

        //get the data in the form of lists
        final List<SleepHmmProtos.StateModel> states = hmmModelData.getStatesList();

        // TODO assert that numStates == length of all the lists above
        final int numStates = hmmModelData.getNumStates();

        //1-D arrays, but that matrix actually corresponds to a numStates x numStates matrix, stored in row-major format
        final List<Double> stateTransitionMatrix = hmmModelData.getStateTransitionMatrixList();
        final List<Double> initialStateProbabilities = hmmModelData.getInitialStateProbabilitiesList();


        //go through list of enums and turn them into sets of ints
        // i.e. state 0 means not sleeping, state 1 means you're sleeping, state 2 means you're sleeping... etc.
        //so later we can say "path[i] is in sleep set?  No? Then you're not sleeping."
        final Set<Integer> sleepStates = new TreeSet<Integer>();
        final Set<Integer> onBedStates = new TreeSet<Integer>();
        final Set<Integer> allowableEndingStates = new TreeSet<Integer>();

        final List<Integer> sleepDepthsByState = new ArrayList<Integer>();


        //Populate the list of composite models
        //each model corresponds to a state---by order it appears in the list.
        //each model (for the moment) is a poisson, poisson, and discrete
        //for light, motion, and waves respectively
        final HmmPdfInterface[] obsModel = new HmmPdfInterface[numStates];

        for (int iState = 0; iState <  numStates; iState++) {

            final SleepHmmProtos.StateModel model = states.get(iState);


            //compose measurement model
            final PdfComposite pdf = new PdfComposite();

            if (model.hasLight()) {
                pdf.addPdf(new GammaPdf(model.getLight().getMean(), model.getLight().getStddev(), HmmDataConstants.LIGHT_INDEX));
            }

            if (model.hasMotionCount()) {
                pdf.addPdf(new PoissonPdf(model.getMotionCount().getMean(), HmmDataConstants.MOT_COUNT_INDEX));
            }

            if (model.hasDisturbances()) {
                pdf.addPdf(new DiscreteAlphabetPdf(model.getDisturbances().getProbabilitiesList(), HmmDataConstants.DISTURBANCE_INDEX));
            }

            if (model.hasLogSoundCount()) {
                pdf.addPdf(new GammaPdf(model.getLogSoundCount().getMean(), model.getLogSoundCount().getStddev(), HmmDataConstants.LOG_SOUND_COUNT_INDEX));
            }

            if (model.hasNaturalLightFilter()) {
                pdf.addPdf(new DiscreteAlphabetPdf(model.getNaturalLightFilter().getProbabilitiesList(), HmmDataConstants.NATURAL_LIGHT_FILTER_INDEX));
            }

            //assign states of onbed, sleeping
            if (model.hasBedMode() && model.getBedMode() == SleepHmmProtos.BedMode.ON_BED) {
                onBedStates.add(iState);
            }


            if (model.hasSleepMode() && model.getSleepMode() == SleepHmmProtos.SleepMode.SLEEP) {
                sleepStates.add(iState);
            }

            //assign allowable ending states
            //#######            BIG ASSUMPTION HERE!!!!     ############
            //allow to end on any state that is NOT SLEEPING
            //because if you're querying this code here... you are sure as fuck not sleeping (until a automated service / robot/ api / skynet does the query)


            if (model.hasSleepMode() && model.getSleepMode() != SleepHmmProtos.SleepMode.SLEEP) {
                allowableEndingStates.add(iState);
            }

            //alternative -- not on bed at all instead of not sleeping
            //if (model.hasBedMode() && model.getBedMode() == SleepHmmProtos.BedMode.OFF_BED) {
            //    allowableEndingStates.add(iState);
            //}



            if (model.hasSleepDepth()) {
                switch (model.getSleepDepth()) {

                    case NOT_APPLICABLE:
                        sleepDepthsByState.add(HmmDataConstants.SLEEP_DEPTH_NONE);
                        break;
                    case LIGHT:
                        sleepDepthsByState.add(HmmDataConstants.SLEEP_DEPTH_LIGHT);
                        break;
                    case REGULAR:
                        sleepDepthsByState.add(HmmDataConstants.SLEEP_DEPTH_REGULAR);
                        break;
                    case DISTURBED:
                        sleepDepthsByState.add(HmmDataConstants.SLEEP_DEPTH_DISTURBED);
                        break;
                    default:
                        sleepDepthsByState.add(HmmDataConstants.SLEEP_DEPTH_NONE);
                        break;
                }
            }
            else {
                sleepDepthsByState.add(HmmDataConstants.SLEEP_DEPTH_NONE);
            }

            obsModel[iState] = pdf;
        }


        //return the HMM + everything else
        final HiddenMarkovModel hmm =  new HiddenMarkovModel(numStates, stateTransitionMatrix, initialStateProbabilities, obsModel,numFreeParams);

        return Optional.of(new NamedSleepHmmModel(hmm,modelName, ImmutableSet.copyOf(sleepStates),ImmutableSet.copyOf(onBedStates),ImmutableSet.copyOf(allowableEndingStates), ImmutableList.copyOf(sleepDepthsByState),
                audioDisturbanceThresoldDB,pillMagnitudeDisturbanceThreshold,naturalLightFilterStartHour,naturalLightFilterStopHour,numMinutesInMeasPeriod,isUsingIntervalSearch,lightPreMultiplier,lightFloorLux,
                useWaveCountsAsDisturbances,audioLevelAboveBackroundThresholdDb));




    }
}

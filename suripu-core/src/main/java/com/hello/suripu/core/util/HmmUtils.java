package com.hello.suripu.core.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by benjo on 2/22/15.
 */
public class HmmUtils {
    final static private int NUM_DATA_DIMENSIONS = 3;
    final static private int LIGHT_INDEX = 0;
    final static private int MOT_COUNT_INDEX = 1;
    final static private int ENERGY_INDEX = 2;
    final static private int SOUND_INDEX = 3;

    final static private int NUM_MINUTES_IN_WINDOW = 15;

    final static private double LIGHT_PREMULTIPLIER = 4.0;



    static public HiddenMarkovModel GetModelFromProtobuf(final SleepHmmProtos.SleepHmm hmmModelData) {
        List<SleepHmmProtos.StateModel> states =  hmmModelData.getStatesList();
        List<SleepHmmProtos.BedMode> bedModes = hmmModelData.getBedModeOfStatesList();
        List<SleepHmmProtos.SleepMode> sleepModes = hmmModelData.getSleepModeOfStatesList();

        final int numStates = hmmModelData.getNumStates();

        List<Double> stateTransitionMatrix = hmmModelData.getStateTransitionMatrixList();
        List<Double> initialStateProbabilities = hmmModelData.getInitialStateProbabilitiesList();


        ArrayList<HmmPdfInterface> obsModel = new  ArrayList<HmmPdfInterface>();
        //TODO do something with the interpretation (bed states and whatnot)
        for (SleepHmmProtos.StateModel model : states) {
            PdfComposite pdf = new PdfComposite();

            pdf.addPdf(new PoissonPdf(model.getLight().getMean(),0));
            pdf.addPdf(new PoissonPdf(model.getMotionCount().getMean(),1));
            pdf.addPdf(new DiscreteAlphabetPdf(model.getWaves().getProbabilitiesList(),2));

            obsModel.add(pdf);

        }


        return new HiddenMarkovModel(numStates,stateTransitionMatrix,initialStateProbabilities,(HmmPdfInterface[])obsModel.toArray());

    }


    static private void maxInBin(double[][] data, long t,double value, final int idx, final long t0, final int numMinutesInWindow) {
        final int tIdx = (int)(t - t0) / 1000 / 60 / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            double v1 = data[idx][tIdx];
            double v2 = value;

            if (v1 < v2) {
                v1 = v2;
            }

            data[idx][tIdx] = v1;
        }

    }

    static private void addToBin(double[][] data, long t,double value, final int idx,final long t0,final int numMinutesInWindow) {
        final int tIdx = (int)(t - t0) / 1000 / 60 / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            data[idx][tIdx] += value;
        }
    }


    static public class BinnedData {
        double [][] data;
        long t0;
        int numMinutesInWindow;
        int timezoneOffset;
    }

    static public class PoissonModel {

        @JsonCreator
        public PoissonModel(@JsonProperty("A") double[][] A,@JsonProperty("obsModel") double [][] obsModel,@JsonProperty("pi") double [] pi, @JsonProperty("myType")String myType) {
            this.A = A;
            this.obsModel = obsModel;
            this.myType = myType;
            this.pi = pi;
        }

        @JsonProperty("myType")
        final String myType;

        @JsonProperty("A")
        final double [][] A;

        @JsonProperty("pi")
        final double [] pi;


        @JsonProperty("obsModel")
        final double [][] obsModel;

    }

    static Optional<PoissonModel> loadfile() {

        Optional<PoissonModel> ret = Optional.absent();

        try {
            ObjectMapper mapper = new ObjectMapper();

            PoissonModel myModel = mapper.readValue(new File("hello.txt").toString(), PoissonModel.class);

            ret = Optional.of(myModel);
        }
        catch (Exception e) {

        }

        return ret;
    }

    List<Optional<Event>> getSleepEventsUsingHMM(AllSensorSampleList sensors, List<TrackerMotion> pillData) {

        List<Optional<Event>> res = new ArrayList<Optional<Event>>();

        Optional<BinnedData> binnedData = getBinnedSensorData(sensors,pillData,NUM_MINUTES_IN_WINDOW);

        if (binnedData.isPresent()) {

            getSleepEventsFromHMM(binnedData.get());
            .


        }

        return  res;
    }

    static public List<Optional<Event>> getSleepEventsFromHMM(final BinnedData data) {

        double [][] A = {{}};
        double [] initialStateProbs = {};
        double [][] poissonMeans = {{},{}};

        Optional<PoissonModel> model = loadfile();

        if (model.isPresent()) {
            HiddenMarkovModel hmm = HiddenMarkovModel.createPoissonOnlyModel(model.get().A, model.get().pi, model.get().obsModel);

            int[] path = hmm.getViterbiPath(data.data);

            interpretPath(path, data);
        }
        //figure out sleep
    }

    static public Optional<BinnedData> getBinnedSensorData(AllSensorSampleList sensors, List<TrackerMotion> pillData, final int numMinutesInWindow) {
        List<Sample> light = sensors.get(Sensor.LIGHT);

        if (light == Collections.EMPTY_LIST || light.isEmpty()) {
            return Optional.absent();
        }

        //get start and end of window
        long t0 = light.get(0).dateTime;
        int timezoneOffset = light.get(0).offsetMillis;
        long tf = light.get(light.size()-1).dateTime;

        int dataLength =(int) (tf-t0) / 1000 / 60 / numMinutesInWindow;

        double [][] data = new double[NUM_DATA_DIMENSIONS][dataLength];

        //zero out data
        for (int i = 0; i < NUM_DATA_DIMENSIONS; i++) {
            Arrays.fill(data[i], 0.0);
        }

        //start filling in the sensor data.  Pick the max of the 5 minute bins for light
        //compute log of light
        Iterator<Sample> it1 = light.iterator();
        while (it1.hasNext()) {
            Sample sample = it1.next();
            double value = sample.value;
            if (value < 0) {
                value = 0.0;
            }

            //TODO transform this back to raw counts before taking log
            value = Math.log(value*LIGHT_PREMULTIPLIER + 1.0) / Math.log(2);

            maxInBin(data,sample.dateTime,value,LIGHT_INDEX,t0,numMinutesInWindow);
        }

        //max of "energy"
        //add counts to bin
        Iterator<TrackerMotion> it2 = pillData.iterator();
        while (it2.hasNext()) {
            TrackerMotion m = it2.next();

            double value = m.value;

            if (value < 0) {
                value = 0;
            }

            value = Math.log(value/2000 + 1);

            addToBin(data,m.timestamp,1.0,MOT_COUNT_INDEX,t0,numMinutesInWindow);
            maxInBin(data,m.timestamp,value,ENERGY_INDEX,t0,numMinutesInWindow);

        }

        BinnedData res = new BinnedData();
        res.data = data;
        res.numMinutesInWindow = numMinutesInWindow;
        res.t0 = t0;
        res.timezoneOffset = timezoneOffset;

        return Optional.of(res);
    }

    static public class  BoundaryResult {
        public int [][] pairs;
        public int [][] disturbances;
    }
    static public int [][]  getSetBoundaries(final int[] path, Set<Integer> inSet, int gapwidth) {
        boolean first = true;

        ArrayList<Integer> t1 = new ArrayList<Integer>();
        ArrayList<Integer> t2 = new ArrayList<Integer>();

        for (int i = 1; i < path.length; i++) {
            int prev = path[i-1];
            int current = path[i];

            if (inSet.contains(current) && !inSet.contains(prev)) {
                first = false;
                t1.add(current); //start

            }

            if (!inSet.contains(current) && inSet.contains(prev) && !first) {
                t2.add(prev); // stop
            }

        }

        int [][] times = new int[2][t2.size()];
        for (int i = 0; i < t2.size(); i++) {
            times[0][i] = t1.get(i);
            times[1][i] = t2.get(i);
        }



        return times;

    }

    DateTime getTimeFromBin(int bin, int binWidthMinutes, long t0, int offset) {
        long t = bin * binWidthMinutes;
        t *= 60 * 1000;
        t += t0;

        DateTime dt = new DateTime(t);
        return dt.withZone(DateTimeZone.forOffsetMillis(offset));
    }

    static public void interpretPath(final int [] path, final BinnedData origdata) {
        Set<Integer> sleepSet = new TreeSet<Integer>();
        sleepSet.add(3);
        sleepSet.add(4);
        sleepSet.add(5);

        Set<Integer> bedSet = new TreeSet<Integer>();
        bedSet.add(1);
        bedSet.add(2);
        bedSet.add(3);
        bedSet.add(4);
        bedSet.add(5);
        bedSet.add(6);

        //make sure that bed-set is a superset of sleepset, otherwise weird shit will happen

        int [][] sleeps = getSetBoundaries(path,sleepSet,30 / origdata.numMinutesInWindow);

        int [][] beds = getSetBoundaries(path,bedSet,30 / origdata.numMinutesInWindow);










    }

}

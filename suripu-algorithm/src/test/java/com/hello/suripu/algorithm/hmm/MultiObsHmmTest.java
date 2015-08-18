package com.hello.suripu.algorithm.hmm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sun.org.apache.xpath.internal.operations.Mult;
import junit.framework.TestCase;
import org.eclipse.jetty.util.MultiMap;
import org.junit.Test;

import java.util.Map;

/**
 * Created by benjo on 8/17/15.
 */
public class MultiObsHmmTest {

    @Test
    public void SimpleDecodeTrainingTest() {


        final double [][] numerator1 = {{0.5,0.5},{0.5,0.5},{0.5,0.5}};
        final double [][] A = {{0.3333,0.3333,0.3333},{0.3333,0.3333,0.3333},{0.3333,0.3333,0.3333}};

        final double [] logDenominator = {0.0,0.0,0.0};


        final double [][] logNumerator1 = LogMath.eln(numerator1);
        final double [][] logA = LogMath.eln(A);

        final double [] pi = {1.0,0.0,0.0};

        final Map<String,double [][]> logNumerators = Maps.newHashMap();
        logNumerators.put("foo1", logNumerator1);

        final MultiObsSequenceAlphabetHiddenMarkovModel hmm = new MultiObsSequenceAlphabetHiddenMarkovModel(logNumerators,logA,logDenominator,pi);

        hmm.scalePriors(1e-50);

        final Map<String, double[][]> rawmeasurements = Maps.newHashMap();
        final Map<Integer, Integer> labels = Maps.newHashMap();
        final Multimap<Integer,MultiObsSequence.Transition> forbiddenTransitions = ArrayListMultimap.create();



        final double [][] raw = {{1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0,
                0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1,
                0, 1, 1, 1,1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1,
                0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1,
                1, 1, 1, 1,0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
                0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,
                0, 0, 0, 0}}; //0.8 for 50, 0.5, for 50, 0.2 for 50

        rawmeasurements.put("foo1",raw);

        for (int i = 0; i < 25; i++) {
            labels.put(i,0);
        }

        for (int i = 50; i < 75; i++) {
            labels.put(i,1);
        }

        for (int i = 100; i < 125; i++) {
            labels.put(i,2);
        }

        final MultiObsSequence multiObsSequence = new MultiObsSequence(rawmeasurements,labels,forbiddenTransitions);



        for (int iter = 0; iter < 100; iter++) {
            hmm.reestimate(multiObsSequence, 1);
        }
        hmm.reestimate(multiObsSequence,1);
        hmm.reestimate(multiObsSequence,1);
        hmm.reestimate(multiObsSequence,1);
        hmm.reestimate(multiObsSequence,1);
        hmm.reestimate(multiObsSequence,1);
        hmm.reestimate(multiObsSequence,1);
        hmm.reestimate(multiObsSequence,1);


        final double [][] transitionMatrix = hmm.getAMatrix();

        final double [][] alphabet = hmm.getAlphabetMatrices().get("foo1");


        double tol = 2e-2;
        TestCase.assertEquals(transitionMatrix[0][0], 0.98,tol);
        TestCase.assertEquals(transitionMatrix[0][1], 0.02, tol);
        TestCase.assertEquals(transitionMatrix[0][2], 0.0, tol);
        TestCase.assertEquals(transitionMatrix[1][0], 0.00, tol);
        TestCase.assertEquals(transitionMatrix[1][1], 0.98, tol);
        TestCase.assertEquals(transitionMatrix[1][2], 0.02, tol);
        TestCase.assertEquals(transitionMatrix[0][2], 0.0, tol);
        TestCase.assertEquals(transitionMatrix[1][2], 0.0, tol);
        TestCase.assertEquals(transitionMatrix[2][2], 1.0, tol);

        tol = 1e-1;
        TestCase.assertEquals(alphabet[0][0],0.2,tol);
        TestCase.assertEquals(alphabet[0][1],0.8,tol);
        TestCase.assertEquals(alphabet[1][0],0.5,tol);
        TestCase.assertEquals(alphabet[1][1],0.5,tol);
        TestCase.assertEquals(alphabet[2][0],0.8,tol);
        TestCase.assertEquals(alphabet[2][1],0.2,tol);


    }

}

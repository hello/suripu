package com.hello.suripu.algorithm.neuralnet;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


/**
 * Created by benjo on 2/23/16.
 */
public class NeuralNetEvaluator {

    final static Logger LOGGER = LoggerFactory.getLogger(NeuralNetEvaluator.class);

    private final MultiLayerNetwork net;

    public static Optional<NeuralNetEvaluator> createFromBinDataAndConfig(final byte [] paramsBinData, final String confData) {
        final MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(confData);
        final MultiLayerNetwork net = new MultiLayerNetwork(confFromJson);

        try {
            //final InputStream ios = Files.newInputStream(Paths.get("/Users/benjo/Downloads/foo.params"));
            final ByteArrayInputStream bis = new ByteArrayInputStream(paramsBinData);
            final DataInputStream dis = new DataInputStream(bis);

            final INDArray params = Nd4j.read(dis);
            net.init();
            net.setParameters(params);

            return Optional.of(new NeuralNetEvaluator(net));

        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return Optional.absent();
    }

    private NeuralNetEvaluator(final MultiLayerNetwork net) {
       this.net = net;
    }

    /*
    *  get output classes
    *  @param x has shape of NUM_STATES x LENGTH_OF_TIME_SERIES
    *
    * */

    public double [][] evaluate(final double [][] x) {
        //Data: has shape [miniBatchSize,nIn,timeSeriesLength];
        //so we are of size [1,nIn,timeSeriesLength]
        if (x.length == 0) {
            return new double[0][0];
        }

        final int N = x.length;
        final int T = x[0].length;


        final INDArray primitiveDataAsInput = Nd4j.create(x);
        final INDArray features = Nd4j.create(Lists.<INDArray>newArrayList(primitiveDataAsInput),new int[]{1,primitiveDataAsInput.size(0),primitiveDataAsInput.size(1)});
        final INDArray output = net.output(features).slice(0);

        final double [][] y = new double[output.shape()[0]][output.shape()[1]];


        for (int i = 0; i < y.length; i++) {
            for (int t = 0; t < y[0].length; t++) {
                y[i][t] = output.getRow(i).getDouble(t);
            }
        }

        return y;
    }
}

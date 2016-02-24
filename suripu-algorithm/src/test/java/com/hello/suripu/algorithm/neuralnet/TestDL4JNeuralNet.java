package com.hello.suripu.algorithm.neuralnet;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by benjo on 2/24/16.
 */
public class TestDL4JNeuralNet {

    @Test
    public void testCreation() {
        final URL paramsUrl = Resources.getResource("neuralnet/2016-02-24T21:06:20.084Z.params");
        final URL configUrl = Resources.getResource("neuralnet/2016-02-24T21:06:20.084Z.config");

        try {
            final byte [] paramsdata = Files.readAllBytes(Paths.get(paramsUrl.toURI()));
            final List<String> configDataLines = Files.readAllLines(Paths.get(configUrl.toURI()), Charset.defaultCharset());

            String configData = "";

            for (final String line : configDataLines) {
                configData += line;
            }


            final Optional<NeuralNetEvaluator> evaluatorOptional =
                    NeuralNetEvaluator.createFromBinDataAndConfig(paramsdata,configData);

            TestCase.assertTrue(evaluatorOptional.isPresent());



        }
        catch (IOException e) {
            TestCase.assertTrue(false);
        }
        catch (URISyntaxException e) {
            TestCase.assertTrue(false);
        }

    }

}

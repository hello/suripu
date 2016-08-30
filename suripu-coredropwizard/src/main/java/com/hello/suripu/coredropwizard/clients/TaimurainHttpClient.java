package com.hello.suripu.coredropwizard.clients;

import com.google.common.base.Optional;
import com.hello.suripu.api.datascience.NeuralNetMessages;
import com.hello.suripu.core.algorithmintegration.NeuralNetAlgorithmOutput;
import com.hello.suripu.core.algorithmintegration.NeuralNetEndpoint;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by benjo on 3/23/16.
 */
public class TaimurainHttpClient implements NeuralNetEndpoint {
    public final static String EVALUATION_ENDPOINT = "/v1/neuralnet/evaluate";
    public static TaimurainHttpClient create(final HttpClient httpClient,final String endpoint) {
       return new TaimurainHttpClient(httpClient,endpoint);
    }

    private final Logger LOGGER = LoggerFactory.getLogger(TaimurainHttpClient.class);
    private final String endpoint;
    final HttpClient httpClient;

    public TaimurainHttpClient(final HttpClient httpClient, final String endpoint) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
    }

    public Optional<NeuralNetAlgorithmOutput> getNetOutput(final String netId, final double [][] sensorData) {

        if (sensorData == null || sensorData.length == 0) {
            return Optional.absent();
        }

        final NeuralNetMessages.NeuralNetInput.Builder builder = NeuralNetMessages.NeuralNetInput.newBuilder();


        builder.setNetId(netId);

        for (int i = 0; i < sensorData.length; i++) {
            final  NeuralNetMessages.DataVector.Builder vecBuilder = NeuralNetMessages.DataVector.newBuilder();
            for (int t = 0; t < sensorData[i].length;t++) {
                vecBuilder.addVec(sensorData[i][t]);
            }

            builder.addMat(vecBuilder);
        }

        final NeuralNetMessages.NeuralNetInput input = builder.build();
        final byte [] payload = input.toByteArray();

        try  {
            final HttpPost httppost = new HttpPost(endpoint + EVALUATION_ENDPOINT);

            httppost.setEntity(new InputStreamEntity(new ByteArrayInputStream(payload), payload.length, ContentType.APPLICATION_OCTET_STREAM));

            final long startTime = DateTime.now().getMillis();
            final HttpResponse response = httpClient.execute(httppost);
            final long endTime = DateTime.now().getMillis();

            LOGGER.info("action=posted_neuralnet duration={} net_id={}",endTime - startTime,netId);

            final NeuralNetMessages.NeuralNetOutput output = NeuralNetMessages.NeuralNetOutput.parseFrom(response.getEntity().getContent());

            final double [][] outputMatrix = new double[output.getMatCount()][0];

            for (int i = 0; i < output.getMatCount(); i++) {
                final List<Double> vec = output.getMat(i).getVecList();

                outputMatrix[i] = new double [vec.size()];

                for (int t = 0; t < vec.size(); t++) {
                    outputMatrix[i][t] = vec.get(t);
                }

            }

            return Optional.of(new NeuralNetAlgorithmOutput(outputMatrix));



        }
        catch (IOException e) {
            LOGGER.error("action=caught_exception exception=IOException message=\"{}\" ",e.getMessage());
            LOGGER.debug(e.getMessage());
        }


        return Optional.absent();

    }
}

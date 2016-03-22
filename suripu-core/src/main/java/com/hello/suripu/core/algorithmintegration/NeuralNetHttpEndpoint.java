package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.hello.suripu.api.datascience.NeuralNetMessages;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by benjo on 3/22/16.
 */
public class NeuralNetHttpEndpoint implements NeuralNetEndpoint {

    private final Logger LOGGER = LoggerFactory.getLogger(NeuralNetHttpEndpoint.class);
    private final String endpoint;

    public NeuralNetHttpEndpoint(final String endpoint) {
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


        try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
            final HttpPost httppost = new HttpPost(endpoint);

            final InputStreamEntity reqEntity = new InputStreamEntity(new ByteArrayInputStream(builder.build().toByteArray()), -1, ContentType.APPLICATION_OCTET_STREAM);
            reqEntity.setChunked(true);

            httppost.setEntity(reqEntity);

            LOGGER.info("Executing request: " + httppost.getRequestLine());

            try(CloseableHttpResponse response = httpclient.execute(httppost)) {
                final NeuralNetMessages.NeuralNetOutput output = NeuralNetMessages.NeuralNetOutput.getDefaultInstance();

                output.parseFrom(response.getEntity().getContent());

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
                LOGGER.error(e.getMessage());
            }

        }
        catch (IOException e) {
            LOGGER.error(e.getMessage());
        }


        return Optional.absent();

    }
}

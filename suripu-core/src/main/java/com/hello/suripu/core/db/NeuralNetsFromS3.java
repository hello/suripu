package com.hello.suripu.core.db;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.hello.suripu.api.datascience.NeuralNetProtos;
import com.hello.suripu.core.models.OnlineHmmPriors;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 2/23/16.
 */
public class NeuralNetsFromS3 implements NeuralNetDAO {


    final static Logger LOGGER = LoggerFactory.getLogger(NeuralNetsFromS3.class);


    public static String getRegularS3ObjectAsString(final AmazonS3 s3, final String bucket, final String key) {
        LOGGER.info("pulling {}/{}",bucket,key);

        final S3Object s3Object = s3.getObject(new GetObjectRequest(bucket, key));

        try (final InputStream stream = s3Object.getObjectContent()) {
            final InputStreamReader inputStreamReader = new InputStreamReader(stream);
            return CharStreams.toString(inputStreamReader);

        }
        catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return "";

    }

    public static  Optional<NeuralNetProtos.NeuralNetMessage> getProtoFromS3(final AmazonS3 s3, final String bucket, final String key) {
        LOGGER.info("pulling {}/{}",bucket,key);

        final S3Object s3Object = s3.getObject(new GetObjectRequest(bucket, key));

        try {
            return Optional.fromNullable(NeuralNetProtos.NeuralNetMessage.parseFrom(s3Object.getObjectContent()));
        }
        catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return Optional.absent();
    }



    private static NeuralNetsFromS3 createFromKeys(final AmazonS3 s3, final String bucket,final List<String> netKeys) {
        final Map<String,NeuralNetProtos.NeuralNetMessage> protos = Maps.newHashMap();

        for (final String key : netKeys) {
            final Optional<NeuralNetProtos.NeuralNetMessage> messageOptional = getProtoFromS3(s3,bucket,key);

            if (!messageOptional.isPresent()) {
                continue;
            }

            final NeuralNetProtos.NeuralNetMessage message = messageOptional.get();

            protos.put(messageOptional.get().getId(),messageOptional.get());
        }

        return new NeuralNetsFromS3(ImmutableMap.copyOf(protos));
    }

    public static NeuralNetsFromS3 createFromConfigBucket(final AmazonS3 s3, final String bucket,final String configKey) {

        final String configData = getRegularS3ObjectAsString(s3,bucket,configKey);

        final String [] keys =  configData.split(System.getProperty("line.separator"));

        final List<String> keysTrimmed = Lists.newArrayList();

        for (final String key : keys) {
            keysTrimmed.add(key.trim());
        }

        return createFromKeys(s3,bucket, keysTrimmed);
    }

    /*---------*/

    final ImmutableMap<String,NeuralNetProtos.NeuralNetMessage> protos;


    private NeuralNetsFromS3(final ImmutableMap<String,NeuralNetProtos.NeuralNetMessage> protos) {
        this.protos = protos;
    }


    @Override
    public Optional<NeuralNetProtos.NeuralNetMessage> getNetDataById(final String id) {
        return Optional.fromNullable(protos.get(id));
    }

    @Override
    public List<String> getAvailableIds() {
        return protos.keySet().asList();
    }
}

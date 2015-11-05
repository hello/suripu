package com.hello.suripu.core.db;
import org.apache.commons.codec.binary.Base64;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import com.hello.suripu.core.models.OnlineHmmPriors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;


/**
 * Created by benjo on 10/20/15.
 */
public class DefaultModelEnsembleFromS3 implements DefaultModelEnsembleDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultModelEnsembleFromS3.class);
    private final OnlineHmmPriors ensemblePriors;
    private final OnlineHmmPriors seedPrior;

    private static OnlineHmmPriors getPriorsFromS3Object(final AmazonS3 s3,final String bucket, final String key) {

        //default
        final S3Object s3Object = s3.getObject(bucket, key);

        try (final Reader inputStream = new InputStreamReader(s3Object.getObjectContent())) {
            final String base64data = CharStreams.toString(inputStream);
            byte [] bindata = Base64.decodeBase64(base64data);
            final int len = bindata.length;

            final Optional<OnlineHmmPriors> priorsOptional = OnlineHmmPriors.createFromProtoBuf(bindata);

            if (!priorsOptional.isPresent()) {
                LOGGER.error("failed to find default models for the ensemble");
                return OnlineHmmPriors.createEmpty();
            }

            LOGGER.info("successfully pulled default models from {}/{}",bucket,key);

            return priorsOptional.get();

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }


        return OnlineHmmPriors.createEmpty();
    }

    public static DefaultModelEnsembleDAO create(final AmazonS3 s3,final String ensembleBucket, final String ensembleKey, final String seedBucket, final String seedKey) {
        final OnlineHmmPriors ensemblePriors = getPriorsFromS3Object(s3,ensembleBucket,ensembleKey);
        final OnlineHmmPriors seedPriors = getPriorsFromS3Object(s3,seedBucket,seedKey);
        return new DefaultModelEnsembleFromS3(ensemblePriors,seedPriors);
    }

    private DefaultModelEnsembleFromS3(final OnlineHmmPriors ensemblePriors,final OnlineHmmPriors seedPriors) {
        this.ensemblePriors = ensemblePriors;
        this.seedPrior = seedPriors;
    }


    @Override
    public OnlineHmmPriors getDefaultModelEnsemble() {
        return ensemblePriors;
    }

    @Override
    public OnlineHmmPriors getSeedModel() {
        return seedPrior;
    }

}

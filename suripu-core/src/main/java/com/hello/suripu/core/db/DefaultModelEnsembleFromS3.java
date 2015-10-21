package com.hello.suripu.core.db;
import org.apache.commons.codec.binary.Base64;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.util.KeyStoreUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;

/**
 * Created by benjo on 10/20/15.
 */
public class DefaultModelEnsembleFromS3 implements DefaultModelEnsembleDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultModelEnsembleFromS3.class);
    private final OnlineHmmPriors priors;

    public static DefaultModelEnsembleDAO create(final AmazonS3 s3,final String bucket, final String key) {
        OnlineHmmPriors onlineHmmPriors = OnlineHmmPriors.createEmpty();

        final S3Object s3Object = s3.getObject(bucket, key);

        final Reader inputStream = new InputStreamReader(s3Object.getObjectContent());

        try {
            final String base64data = CharStreams.toString(inputStream);
            byte [] bindata = Base64.decodeBase64(base64data);
            final int len = bindata.length;

            final Optional<OnlineHmmPriors> priorsOptional = OnlineHmmPriors.createFromProtoBuf(bindata);

            if (priorsOptional.isPresent()) {
                onlineHmmPriors = priorsOptional.get();
            }
            else {
                LOGGER.error("failed to find default models for the ensemble");
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return new DefaultModelEnsembleFromS3(onlineHmmPriors);
    }

    private DefaultModelEnsembleFromS3(final OnlineHmmPriors priors) {
        this.priors = priors;
    }


    @Override
    public OnlineHmmPriors getDefaultModel() {
        return priors;
    }
}

package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.OnlineHmmPriors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by benjo on 10/17/15.
 */
public class DefaultModelEnsembleDAOFromFile implements DefaultModelEnsembleDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultModelEnsembleDAO.class);
    private Optional<OnlineHmmPriors> priorsOptional;
    private final String filepath;

    public DefaultModelEnsembleDAOFromFile(final String filepathToModel) {
        priorsOptional = Optional.absent();
        filepath = filepathToModel;
    }

    private  byte[] loadFile(String filepath) throws IOException {
        final File file = new File(filepath);
        final InputStream is = new FileInputStream(file);

        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            LOGGER.error("file is too large");
        }
        byte[] bytes = new byte[(int)length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }

        is.close();
        return bytes;
    }

    @Override
    public OnlineHmmPriors getDefaultModel() {

        if (!priorsOptional.isPresent()) {
            LOGGER.info("attempting to load model file: {}",filepath);

            try {
                priorsOptional = OnlineHmmPriors.createFromProtoBuf(loadFile(filepath));
            } catch (IOException e) {
                LOGGER.error("{}",e);
            }
        }

        if (!priorsOptional.isPresent()) {
            return OnlineHmmPriors.createEmpty();
        }

        return priorsOptional.get();
    }
}

package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.OnlineHmmPriors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *  Intended behavior is to read from the file system each time
 *  so that if you change the model file the changes will be reflected
 *  during the debugging.
 */
public class DefaultModelEnsembleDAOFromFile implements DefaultModelEnsembleDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultModelEnsembleDAO.class);

    private OnlineHmmPriors ensemblePriors;
    private OnlineHmmPriors seedPriors;

    private final String filepathToEnsemble;
    private final String filepathToSeedModel;

    public DefaultModelEnsembleDAOFromFile(final String filePathToEnsemble,final String filepathToSeedModel) {
        ensemblePriors = OnlineHmmPriors.createEmpty();
        this.filepathToEnsemble = filePathToEnsemble;
        this.filepathToSeedModel = filepathToSeedModel;
    }

    private  byte[] loadFile(String filepath)  {
        byte[] bytes = new byte[0];

        final File file = new File(filepath);

        try (final InputStream is = new FileInputStream(file)) {
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                throw new IOException("file is tool arge");
            }
            bytes = new byte[(int) length];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }

        }
        catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return bytes;
    }

    private OnlineHmmPriors readModel(final String filepath) {

        LOGGER.info("attempting to load model file: {}", filepath);
        final Optional<OnlineHmmPriors> priors = OnlineHmmPriors.createFromProtoBuf(loadFile(filepath));

        if (priors.isPresent()) {
            return priors.get();
        }

        return OnlineHmmPriors.createEmpty();
    }

    @Override
    public OnlineHmmPriors getDefaultModelEnsemble() {
        return readModel(filepathToEnsemble);
    }

    @Override
    public OnlineHmmPriors getSeedModel() {
        return readModel(filepathToSeedModel);
    }

}

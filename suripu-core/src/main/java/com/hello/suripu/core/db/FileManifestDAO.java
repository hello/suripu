package com.hello.suripu.core.db;

import com.google.common.base.Optional;

/**
 * Created by jakepiccolo on 3/8/16.
 */
public interface FileManifestDAO {

    class FileManifest {}

    /**
     * @param newManifest - new manifest to set
     * @return The old manifest
     */
    Optional<FileManifest> updateManifest(final FileManifest newManifest);

    Optional<FileManifest> getManifest(final String senseId);
}

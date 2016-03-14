package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.api.input.FileSync;

/**
 * Created by jakepiccolo on 3/8/16.
 */
public interface FileManifestDAO {

    /**
     * @param newManifest - new manifest to set
     * @return The old manifest
     */
    Optional<FileSync.FileManifest> updateManifest(final String senseId, final FileSync.FileManifest newManifest);

    Optional<FileSync.FileManifest> getManifest(final String senseId);
}

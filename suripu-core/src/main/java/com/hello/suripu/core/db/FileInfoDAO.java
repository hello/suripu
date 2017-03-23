package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.FileInfo;

import java.util.List;

/**
 * Created by ksg on 3/22/17
 */
public interface FileInfoDAO {
    Optional<FileInfo> getById(final Long id);

    List<FileInfo> getAll(final Integer firmwareVersion, final String senseId);

    List<FileInfo> getAllForType(final FileInfo.FileType fileType);

    Optional<FileInfo> getByFilePath(final String filePath);

    Optional<FileInfo> getByFileName(final String fileName);

}

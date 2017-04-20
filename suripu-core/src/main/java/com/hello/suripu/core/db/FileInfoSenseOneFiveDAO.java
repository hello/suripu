package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.mappers.FileInfoMapper;
import com.hello.suripu.core.models.FileInfo;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ksg, copied from FileInfoDAO, mostly for re-downloading corrupt files
 * All values are the same as file_info except these columns: uri, sha, and size_bytes
 */
@RegisterMapper(FileInfoMapper.class)
public abstract class FileInfoSenseOneFiveDAO implements FileInfoDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileInfoSenseOneFiveDAO.class);

    protected static final Integer OLD_FW_VERSION_CUTOFF = 100000000;

    @SqlQuery("SELECT * FROM file_info_one_five WHERE id=:id LIMIT 1;")
    @SingleValueResult(FileInfo.class)
    public abstract Optional<FileInfo> getById(@Bind("id") final Long id);


    @SqlQuery("SELECT DISTINCT(fi.*) " +
            "FROM file_info_one_five AS fi " +
            "LEFT JOIN sense_file_info_one_five AS sfi " +
            "ON fi.id=sfi.file_info_id " +
            "WHERE (is_public AND firmware_version <= :firmware_version) OR sense_id=:sense_id " +
            "ORDER BY sort_key;")
    protected abstract List<FileInfo> getAllForFirmwareVersionAndSenseId(
            @Bind("firmware_version") final Integer firmwareVersion,
            @Bind("sense_id") final String senseId);


    @SqlQuery("SELECT * FROM file_info_one_five WHERE type=:file_type ORDER BY sort_key;")
    public abstract List<FileInfo> getAllForType(@Bind("file_type") final FileInfo.FileType fileType);



    public List<FileInfo> getAll(final Integer firmwareVersion, final String senseId) {
        if (firmwareVersion >= OLD_FW_VERSION_CUTOFF) {
            return Lists.newArrayList();
        }
        return getAllForFirmwareVersionAndSenseId(firmwareVersion, senseId);
    }


    @SqlQuery("SELECT * FROM file_info_one_five WHERE path=:file_path LIMIT 1;")
    @SingleValueResult(FileInfo.class)
    public abstract Optional<FileInfo> getByFilePath(@Bind("file_path") final String filePath);

    @SqlQuery("SELECT * FROM file_info_one_five WHERE name=:name LIMIT 1;")
    @SingleValueResult(FileInfo.class)
    public abstract Optional<FileInfo> getByFileName(@Bind("name") final String fileName);

}

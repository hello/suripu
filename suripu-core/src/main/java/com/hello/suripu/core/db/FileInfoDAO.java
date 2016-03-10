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
 * Created by jakepiccolo on 3/9/16.
 */
@RegisterMapper(FileInfoMapper.class)
public abstract class FileInfoDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileInfoDAO.class);

    private static final Long OLD_FW_VERSION_CUTOFF = 100000000L;

    @SqlQuery("SELECT * FROM file_info WHERE id=:id LIMIT 1;")
    @SingleValueResult(FileInfo.class)
    public abstract Optional<FileInfo> getById(@Bind("id") final Long id);



    @SqlQuery("SELECT fi.* " +
            "FROM file_info AS fi " +
            "LEFT JOIN sense_file_info AS sfi " +
            "ON fi.id=sfi.file_info_id " +
            "WHERE (is_public AND firmware_version <= :firmware_version) OR sense_id=:sense_id;")
    protected abstract List<FileInfo> getAllForFirmwareVersionAndSenseId(
            @Bind("firmware_version") final Long firmwareVersion,
            @Bind("sense_id") final String senseId);



    public List<FileInfo> getAll(final Long firmwareVersion, final String senseId) {
        if (firmwareVersion > OLD_FW_VERSION_CUTOFF) {
            return Lists.newArrayList();
        }
        return getAllForFirmwareVersionAndSenseId(firmwareVersion, senseId);
    }


    @SqlQuery("SELECT * FROM file_info WHERE path=:file_path LIMIT 1;")
    @SingleValueResult(FileInfo.class)
    public abstract Optional<FileInfo> getByFilePath(@Bind("file_path") final String filePath);

}

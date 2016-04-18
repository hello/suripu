package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.FileInfo;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jakepiccolo on 3/9/16.
 */
public class FileInfoMapper implements ResultSetMapper<FileInfo> {

    @Override
    public FileInfo map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return FileInfo.newBuilder()
                .withId(resultSet.getLong("id"))
                .withFileType(FileInfo.FileType.valueOf(resultSet.getString("type")))
                .withPath(resultSet.getString("path"))
                .withSha(resultSet.getString("sha"))
                .withUri(resultSet.getString("uri"))
                .withPreviewUri(resultSet.getString("preview_uri"))
                .withName(resultSet.getString("name"))
                .withFirmwareVersion(resultSet.getInt("firmware_version"))
                .withIsPublic(resultSet.getBoolean("is_public"))
                .withSizeBytes(resultSet.getInt("size_bytes"))
                .build();
    }
}

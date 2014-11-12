package com.hello.suripu.core.firmware;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FileDownloadMapper implements ResultSetMapper<FirmwareFile> {
    @Override
    public FirmwareFile map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new FirmwareFile(
                r.getString("s3_bucket"),
                r.getString("s3_key"),
                r.getBoolean("copy_to_serial_flash"),
                r.getBoolean("reset_network_processor"),
                r.getBoolean("reset_application_processor"),
                r.getString("serial_flash_filename"),
                r.getString("serial_flash_path"),
                r.getString("sd_card_filename"),
                r.getString("sd_card_path")
        );
    }
}

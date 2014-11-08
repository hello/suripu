package com.hello.suripu.core.firmware;

import com.google.common.collect.ImmutableList;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(FileDownloadMapper.class)
public interface FirmwareUpdateDAO {


    @SqlQuery("SELECT * FROM firmware_updates WHERE device_id = :device_id AND firmware_version > :current_firmware_version;")
    public ImmutableList<FirmwareFile> getFiles(@Bind("device_id") String deviceId, @Bind("current_firmware_version") Integer currentFirmwareVersion);


    @SqlUpdate("INSERT INTO firmware_updates(device_id, firmware_version, s3_bucket, s3_key, copy_to_serial_flash, reset_network_processor, reset_application_processor, " +
            "serial_flash_filename, serial_flash_path, sd_card_filename, sd_card_path) " +
            "VALUES(:device_id, :firmware_version, :s3_bucket, :s3_key, :copy_to_serial_flash, :reset_network_processor, :reset_application_processor, " +
            ":serial_flash_filename, :serial_flash_path, :sd_card_filename, :sd_card_path);")
    public void insert(@BindFirmwareFile FirmwareFile firmwareFile, @Bind("device_id") String deviceId, @Bind("firmware_version") Integer firmwareVersion);

    @SqlUpdate("DELETE FROM firmware_updates WHERE device_id = :device_id")
    public void reset(@Bind("device_id") String deviceId);


}

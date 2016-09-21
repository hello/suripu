package com.hello.suripu.core.db;

import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.FileInfo;
import org.junit.Test;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by jakepiccolo on 3/9/16.
 */
public class FileInfoDAOTest extends SqlDAOTest<FileInfoDAO> {

    @Override
    protected Class<FileInfoDAO> tClass() {
        return FileInfoDAO.class;
    }

    @Override
    protected String tearDownQuery() {
        return "DROP TABLE file_info; DROP TABLE sense_file_info;";
    }

    @Override
    protected String setupQuery() {
        return "CREATE TABLE file_info (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    sort_key INTEGER NOT NULL,          -- How to sort the values for displaying\n" +
                "    firmware_version INTEGER NOT NULL,  -- Minimum firmware version\n" +
                "    type VARCHAR(255),\n" +
                "    path VARCHAR(255),\n" +
                "    sha VARCHAR(255),\n" +
                "    uri VARCHAR(255),\n" +
                "    preview_uri VARCHAR(255),\n" +
                "    name VARCHAR(255),\n" +
                "    is_public BOOLEAN DEFAULT FALSE,\n" +
                "    size_bytes INTEGER,\n" +
                "    hardware_version INTEGER NOT NULL DEFAULT 1\n" +
                ");\n" +
                "\n" +
                "CREATE TABLE sense_file_info (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    sense_id VARCHAR(255) NOT NULL,\n" +
                "    file_info_id INTEGER NOT NULL REFERENCES file_info (id)\n" +
                ");";
    }

    private void insert(final Long id, final Integer sortKey, final Integer firmwareVersion, final Boolean isPublic, final HardwareVersion hardwareVersion)
    {
        final String insertStatement = "Insert INTO file_info (id, sort_key, firmware_version, is_public, type, path, sha, uri, preview_uri, name, size_bytes, hardware_version)\n" +
                "VALUES (:id, :sort_key, :firmware_version, :is_public, 'SLEEP_SOUND', '', '', '', '', '', 0, :hardware_version);";
        handle.createStatement(insertStatement)
                .bind("id", id)
                .bind("sort_key", sortKey)
                .bind("firmware_version", firmwareVersion)
                .bind("is_public", isPublic)
                .bind("hardware_version", hardwareVersion.value)
                .execute();
    }

    private void insert(final Long id, final Integer sortKey, final Integer firmwareVersion, final Boolean isPublic, final HardwareVersion hardwareVersion, final String... senseIds)
    {
        insert(id, sortKey, firmwareVersion, isPublic, hardwareVersion);
        for (final String senseId: senseIds) {
            handle.createStatement("INSERT INTO sense_file_info (file_info_id, sense_id)\n" +
                    "VALUES (:id, :sense_id);")
                    .bind("id", id)
                    .bind("sense_id", senseId)
                    .execute();
        }
    }

    @Test
    public void testGetAll() throws Exception {
        // Empty table
        assertThat(dao.getAll(1, HardwareVersion.SENSE_ONE, "").size(), is(0));

        // Insert a couple of rows
        insert(1L, 1, 5, true, HardwareVersion.SENSE_ONE);
        insert(2L, 200, 5, true, HardwareVersion.SENSE_ONE);
        insert(3L, 3, 6, true, HardwareVersion.SENSE_ONE);
        insert(4L, 4, 1, true, HardwareVersion.SENSE_ONE);
        insert(5L, 5, 1, false, HardwareVersion.SENSE_ONE, "sense1");
        insert(6L, 6, 1, false, HardwareVersion.SENSE_ONE, "sense2");
        insert(7L, 7, 1000, false, HardwareVersion.SENSE_ONE, "sense1");
        insert(8L, 8, 1, true, HardwareVersion.SENSE_ONE, "sense1", "sense2");


        final List<FileInfo> after0 = dao.getAll(0, HardwareVersion.SENSE_ONE, "nosense");
        assertThat(after0.size(), is(0));

        final List<FileInfo> after1 = dao.getAll(1, HardwareVersion.SENSE_ONE, "nosense");
        assertThat(after1.size(), is(2));
        assertThat(after1.get(0).id, is(4L));
        assertThat(after1.get(1).id, is(8L));

        final List<FileInfo> after5 = dao.getAll(5, HardwareVersion.SENSE_ONE, "nosense");
        assertThat(after5.size(), is(4));
        assertThat(after5.get(0).id, is(1L));
        assertThat(after5.get(1).id, is(4L));
        assertThat(after5.get(2).id, is(8L));
        assertThat(after5.get(3).id, is(2L));

        assertThat(dao.getAll(FileInfoDAO.OLD_FW_VERSION_CUTOFF, HardwareVersion.SENSE_ONE, "nosense").size(), is(0));

        // test specific sense ids
        final List<FileInfo> forSense1 = dao.getAll(3, HardwareVersion.SENSE_ONE, "sense1");
        assertThat(forSense1.size(), is(4));
        assertThat(forSense1.get(0).id, is(4L));
        assertThat(forSense1.get(1).id, is(5L));
        assertThat(forSense1.get(2).id, is(7L));
        assertThat(forSense1.get(3).id, is(8L));

        final List<FileInfo> forSense2 = dao.getAll(5, HardwareVersion.SENSE_ONE, "sense2");
        assertThat(forSense2.size(), is(5));
        assertThat(forSense2.get(0).id, is(1L));
        assertThat(forSense2.get(1).id, is(4L));
        assertThat(forSense2.get(2).id, is(6L));
        assertThat(forSense2.get(3).id, is(8L));
        assertThat(forSense2.get(4).id, is(2L));


        final List<FileInfo> beforeSenseOneFive = dao.getAll(1, HardwareVersion.SENSE_ONE_FIVE, "sense_one_five");
        assertTrue("beforeSenseOneFive empty", beforeSenseOneFive.isEmpty());
        insert(9L, 9, 1, true, HardwareVersion.SENSE_ONE_FIVE, "sense_one_five");
        final List<FileInfo> afterSenseOneFive = dao.getAll(1, HardwareVersion.SENSE_ONE_FIVE, "foo");
        assertEquals("after insert", afterSenseOneFive.size(), 1);

        final List<FileInfo> wrongHardwareVersion = dao.getAll(1, HardwareVersion.SENSE_ONE, "sense_one_five");
        assertFalse("wrongHardwareVersion empty", wrongHardwareVersion.isEmpty());
    }



    @Test
    public void testGetAllForType() throws Exception {
        // Empty table
        assertThat(dao.getAllForType(FileInfo.FileType.SLEEP_SOUND).size(), is(0));

        // Insert sleep sound
        insert(1L, 1, 5, true, HardwareVersion.SENSE_ONE);

        assertThat(dao.getAllForType(FileInfo.FileType.SLEEP_SOUND).get(0).id, is(1L));
        assertThat(dao.getAllForType(FileInfo.FileType.ALARM).size(), is(0));
    }

}
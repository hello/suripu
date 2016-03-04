package com.hello.suripu.core.db.sleep_sounds;

import com.hello.suripu.core.db.mappers.GroupedTimelineLogsSummaryMapper;
import com.hello.suripu.core.models.sleep_sounds.Sound;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 3/3/16.
 */
public class SoundDAOTest {

    private DBI dbi;
    private Handle handle;
    private SoundDAO dao;

    @Before
    public void setUp() throws Exception {
        final String createTableQuery = "CREATE TABLE sleep_sounds (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    preview_url VARCHAR(255) NOT NULL,  -- Preview for app\n" +
                "    name VARCHAR(255) NOT NULL,         -- Display name\n" +
                "    file_path VARCHAR(255) NOT NULL,    -- Path on Sense\n" +
                "    url VARCHAR(255) NOT NULL,          -- Path to full file\n" +
                "    sort_key INTEGER NOT NULL,          -- How to sort the values for displaying\n" +
                "    firmware_version INTEGER NOT NULL   -- Minimum firmware version\n" +
                ");";

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new GroupedTimelineLogsSummaryMapper());
        handle = dbi.open();

        handle.execute(createTableQuery);
        dao = dbi.onDemand(SoundDAO.class);
    }

    @After
    public void tearDown() throws Exception {
        handle.execute("DROP TABLE sleep_sounds;");
        handle.close();
    }

    private void insert(final Long id, final String previewUrl, final String name, final String filePath,
                        final String url, final Integer sortKey, final Integer firmwareVersion)
    {
        // Use String.format to build SQL cause this is a test LMFAO
        handle.execute(String.format(
                "Insert INTO sleep_sounds (id, preview_url, name, file_path, url, sort_key, firmware_version)\n" +
                "VALUES (%s, '%s', '%s', '%s', '%s', %s, %s);",
                id, previewUrl, name, filePath, url, sortKey, firmwareVersion));
    }

    @Test
    public void testGetAllForFirmwareVersionExcludingOldVersions() throws Exception {
        // Empty table
        assertThat(dao.getAllForFirmwareVersionExcludingOldVersions(1).size(), is(0));

        // Insert a couple of rows
        insert(1L, "p1", "n1", "path1", "url1", 1, 5);
        insert(2L, "p2", "n2", "path2", "url2", 200, 5);
        insert(3L, "p3", "n3", "path3", "url3", 3, 6);
        insert(4L, "p4", "n4", "path4", "url4", 4, 1);

        final List<Sound> after0 = dao.getAllForFirmwareVersionExcludingOldVersions(0);
        assertThat(after0.size(), is(0));

        final List<Sound> after1 = dao.getAllForFirmwareVersionExcludingOldVersions(1);
        assertThat(after1.size(), is(1));
        assertThat(after1.get(0).id, is(4L));

        final List<Sound> after5 = dao.getAllForFirmwareVersionExcludingOldVersions(5);
        assertThat(after5.size(), is(3));
        assertThat(after5.get(0).id, is(1L));
        assertThat(after5.get(1).id, is(4L));
        assertThat(after5.get(2).id, is(2L));

        assertThat(dao.getAllForFirmwareVersionExcludingOldVersions(SoundDAO.OLD_FW_VERSION_CUTOFF).size(), is(0));
    }

    @Test
    public void testHasSoundEnabledExcludingOldFirmwareVersions() throws Exception {
        insert(1L, "p1", "n1", "path1", "url1", 1, 5);
        insert(2L, "p2", "n2", "path2", "url2", 200, 5);
        insert(3L, "p3", "n3", "path3", "url3", 3, 6);
        insert(4L, "p4", "n4", "path4", "url4", 4, 1);

        assertThat(dao.hasSoundEnabledExcludingOldFirmwareVersions(1L, 5), is(true));
        assertThat(dao.hasSoundEnabledExcludingOldFirmwareVersions(3L, 4), is(false));
        assertThat(dao.hasSoundEnabledExcludingOldFirmwareVersions(1L, 4), is(false));
        assertThat(dao.hasSoundEnabledExcludingOldFirmwareVersions(4L, SoundDAO.OLD_FW_VERSION_CUTOFF), is(false));
    }

}
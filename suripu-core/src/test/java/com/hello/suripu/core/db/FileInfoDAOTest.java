package com.hello.suripu.core.db;

import com.hello.suripu.core.db.mappers.GroupedTimelineLogsSummaryMapper;
import com.hello.suripu.core.models.FileInfo;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 3/9/16.
 */
public class FileInfoDAOTest {

    private DBI dbi;
    private Handle handle;
    private FileInfoDAO dao;

    @Before
    public void setUp() throws Exception {
        final String createTableQuery = "CREATE TABLE file_info (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    sort_key INTEGER NOT NULL,          -- How to sort the values for displaying\n" +
                "    firmware_version INTEGER NOT NULL,  -- Minimum firmware version\n" +
                "    type VARCHAR(255),\n" +
                "    path VARCHAR(255),\n" +
                "    sha VARCHAR(255),\n" +
                "    uri VARCHAR(255),\n" +
                "    preview_uri VARCHAR(255),\n" +
                "    name VARCHAR(255),\n" +
                "    is_public BOOLEAN DEFAULT FALSE\n" +
                ");\n" +
                "\n" +
                "CREATE TABLE sense_file_info (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    sense_id VARCHAR(255) NOT NULL,\n" +
                "    file_info_id INTEGER NOT NULL REFERENCES file_info (id)\n" +
                ");";

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new GroupedTimelineLogsSummaryMapper());
        handle = dbi.open();

        handle.execute(createTableQuery);
        dao = dbi.onDemand(FileInfoDAO.class);
    }

    @After
    public void tearDown() throws Exception {
        handle.execute("DROP TABLE file_info;");
        handle.close();
    }


    private void insert(final Long id, final Integer sortKey, final Integer firmwareVersion, final Boolean isPublic)
    {
        // Use String.format to build SQL cause this is a test LMFAO
        handle.execute(String.format(
                "Insert INTO file_info (id, sort_key, firmware_version, is_public, type, path, sha, uri, preview_uri, name)\n" +
                        "VALUES (%s, %s, %s, %s, 'SLEEP_SOUND', '', '', '', '', '');",
                id, sortKey, firmwareVersion, isPublic));
    }

    private void insert(final Long id, final Integer sortKey, final Integer firmwareVersion, final Boolean isPublic, final String senseId)
    {
        insert(id, sortKey, firmwareVersion, isPublic);
        handle.execute(String.format(
                "INSERT INTO sense_file_info (file_info_id, sense_id)\n" +
                        "VALUES (%s, '%s');",
                id, senseId
        ));
    }

    @Test
    public void testGetAll() throws Exception {
        // Empty table
        assertThat(dao.getAll(1, "").size(), is(0));

        // Insert a couple of rows
        insert(1L, 1, 5, true);
        insert(2L, 200, 5, true);
        insert(3L, 3, 6, true);
        insert(4L, 4, 1, true);
        insert(5L, 5, 1, false, "sense1");
        insert(6L, 6, 1, false, "sense2");
        insert(7L, 7, 1000, false, "sense1");

        final List<FileInfo> after0 = dao.getAll(0, "nosense");
        assertThat(after0.size(), is(0));

        final List<FileInfo> after1 = dao.getAll(1, "nosense");
        assertThat(after1.size(), is(1));
        assertThat(after1.get(0).id, is(4L));

        final List<FileInfo> after5 = dao.getAll(5, "nosense");
        assertThat(after5.size(), is(3));
        assertThat(after5.get(0).id, is(1L));
        assertThat(after5.get(1).id, is(4L));
        assertThat(after5.get(2).id, is(2L));

        assertThat(dao.getAll(FileInfoDAO.OLD_FW_VERSION_CUTOFF, "nosense").size(), is(0));

        // TODO test specific sense ids
    }

}
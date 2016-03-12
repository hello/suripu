package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.google.common.base.Optional;
import com.hello.suripu.api.input.FileSync;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 3/11/16.
 */
public class FileManifestDynamoDBIT extends DynamoDBIT<FileManifestDynamoDB> {

    @Override
    protected CreateTableResult createTable() {
        return dao.createTable(2L, 2L);
    }

    @Override
    protected FileManifestDynamoDB createDAO() {
        return new FileManifestDynamoDB(amazonDynamoDBClient, TABLE_NAME);
    }

    @Test
    public void testUpdateAndGetManifest() throws Exception {
        final String senseId = "sense";

        final FileSync.FileManifest initialManifest = FileSync.FileManifest.newBuilder()
                .setSenseId(senseId)
                .setDeviceUptime(100)
                .build();

        final Optional<FileSync.FileManifest> noManifest = dao.updateManifest(senseId, initialManifest);
        assertThat(noManifest.isPresent(), is(false));

        final FileSync.FileManifest nextManifest = FileSync.FileManifest.newBuilder()
                .setSenseId(senseId)
                .setDeviceUptime(200) // Same as previous but with new uptime
                .build();

        // Putting a new state should return the prior state
        final Optional<FileSync.FileManifest> initialManifestRetrieved = dao.updateManifest(senseId, nextManifest);
        assertThat(initialManifestRetrieved.isPresent(), is(true));
        assertThat(initialManifestRetrieved.get(), is(initialManifest));

        // Should be able to get the latest state
        assertThat(dao.getManifest(senseId).get(), is(nextManifest));
    }
}
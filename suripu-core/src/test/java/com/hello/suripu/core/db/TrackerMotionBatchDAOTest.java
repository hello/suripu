package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.db.util.PostgresIntegerArrayArgumentFactory;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Created by pangwu on 6/23/14.
 */
public class TrackerMotionBatchDAOTest {

    private TrackerMotionBatchDAO trackerMotionBatchDAO;

    @Before
    public void setup() {
        // TODO: Create a WIKI to describe how this shit works.

        cleanUp();

        final DBI dbi = new DBI("jdbc:postgresql://localhost:5432/pangwu", "pangwu", "");
        dbi.registerArgumentFactory(new JodaArgumentFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());
        dbi.registerArgumentFactory(new PostgresIntegerArrayArgumentFactory());

        // the following two factories are registered by Dropwizard by default..
        // Shall we make them added explicitly in our Services?
        dbi.registerContainerFactory(new ImmutableListContainerFactory());
        dbi.registerContainerFactory(new ImmutableSetContainerFactory());

        this.trackerMotionBatchDAO = dbi.onDemand(TrackerMotionBatchDAO.class);
    }

    @After
    public void cleanUp(){
        final DBI dbi = new DBI("jdbc:postgresql://localhost:5432/pangwu", "pangwu", "");
        final Handle handle = dbi.open();
        handle.execute("DELETE FROM motion_batch WHERE account_id = -1;");
        handle.close();

    }


    @Test
    public void insertTest(){
        final DateTime startDateTime = DateTime.now();
        final ArrayList<TrackerMotion> motions = new ArrayList<>();
        for(int i = 0; i < 11; i++){
            motions.add(new TrackerMotion(-1, startDateTime.getMillis() + i * 60 * 1000, i, startDateTime.getZone().getOffset(startDateTime)));

        }

        final TrackerMotion.Batch expectedBatch = new TrackerMotion.Batch(-1, startDateTime.getMillis(), startDateTime.getZone().getOffset(startDateTime), motions);
        this.trackerMotionBatchDAO.insert(expectedBatch);

        final ImmutableList<TrackerMotion.Batch> batches = this.trackerMotionBatchDAO.getBetween(-1L,
                startDateTime,
                startDateTime.plusDays(1));

        final ArrayList<TrackerMotion> actual = new ArrayList<>();
        for(final TrackerMotion.Batch batch:batches){
            for(final TrackerMotion trackerMotion:batch.motionData){
                actual.add(trackerMotion);
            }
        }

        assertThat(motions, containsInAnyOrder(actual.toArray()));
    }

    @Test
    public void getBetweenTest(){
        final DateTime startDateTime = DateTime.now();
        final ArrayList<TrackerMotion> motions = new ArrayList<>();

        // Today's data
        final TrackerMotion trackerMotion4Today = new TrackerMotion(-1, startDateTime.getMillis(), 100, startDateTime.getZone().getOffset(startDateTime));
        motions.add(trackerMotion4Today);

        final TrackerMotion.Batch expectedBatch4Today = new TrackerMotion.Batch(-1, startDateTime.getMillis(), startDateTime.getZone().getOffset(startDateTime), motions);
        this.trackerMotionBatchDAO.insert(expectedBatch4Today);


        motions.clear();

        // Tomorrow's data
        final TrackerMotion trackerMotion4Tomorrow = new TrackerMotion(-1, startDateTime.plusDays(1).getMillis(), 101, startDateTime.plusDays(1).getZone().getOffset(startDateTime.plusDays(1)));
        motions.add(trackerMotion4Tomorrow);
        final TrackerMotion.Batch expectedBatch4Tomorrow = new TrackerMotion.Batch(-1, startDateTime.plusDays(1).getMillis(), startDateTime.plusDays(1).getZone().getOffset(startDateTime.plusDays(1)), motions);
        this.trackerMotionBatchDAO.insert(expectedBatch4Tomorrow);

        final DateTime[] queryRange4Today = new DateTime[] {startDateTime, startDateTime.plusHours(12)};

        // Query for today's data, should get only today's data
        final ImmutableList<TrackerMotion.Batch> batches4Today = this.trackerMotionBatchDAO.getBetween(-1L,
                queryRange4Today[0],
                queryRange4Today[1]);

        final ArrayList<TrackerMotion> actual4Today = new ArrayList<>();
        for(final TrackerMotion.Batch batch:batches4Today){
            for(final TrackerMotion trackerMotion:batch.motionData){
                actual4Today.add(trackerMotion);
            }
        }

        final ArrayList<TrackerMotion> expected4Today = new ArrayList<>();
        expected4Today.add(trackerMotion4Today);
        assertThat(expected4Today, containsInAnyOrder(actual4Today.toArray()));


        // Query for tomorrow's data, should return tomorrow's data
        final DateTime[] queryRange4Tomorrow = new DateTime[] {startDateTime.plusHours(12), startDateTime.plusHours(24)};

        final ImmutableList<TrackerMotion.Batch> batches4Tomorrow = this.trackerMotionBatchDAO.getBetween(-1L,
                queryRange4Tomorrow[0],
                queryRange4Tomorrow[1]);

        final ArrayList<TrackerMotion> actual4Tomorrow = new ArrayList<>();
        for(final TrackerMotion.Batch batch:batches4Tomorrow){
            for(final TrackerMotion trackerMotion:batch.motionData){
                actual4Tomorrow.add(trackerMotion);
            }
        }

        final ArrayList<TrackerMotion> expected4Tomorrow = new ArrayList<>();
        expected4Tomorrow.add(trackerMotion4Tomorrow);
        assertThat(expected4Tomorrow, containsInAnyOrder(actual4Tomorrow.toArray()));

        // Query for both days' data, should return everything.
        final DateTime[] queryRange4All = new DateTime[] {startDateTime, startDateTime.plusDays(1)};
        final ImmutableList<TrackerMotion.Batch> allBatches = this.trackerMotionBatchDAO.getBetween(-1L,
                queryRange4All[0],
                queryRange4All[1]);

        final ArrayList<TrackerMotion> actual4All = new ArrayList<>();
        for(final TrackerMotion.Batch batch:allBatches){
            for(final TrackerMotion trackerMotion:batch.motionData){
                actual4All.add(trackerMotion);
            }
        }

        final ArrayList<TrackerMotion> expected4All = new ArrayList<>();
        expected4All.add(trackerMotion4Today);
        expected4All.add(trackerMotion4Tomorrow);

        assertThat(expected4All, containsInAnyOrder(actual4All.toArray()));


        // Query data for 3 days later, should return empty
        final DateTime[] queryFor3daysLater = new DateTime[] {startDateTime.plusDays(3), startDateTime.plusDays(4)};
        final ImmutableList<TrackerMotion.Batch> emptyBatches = this.trackerMotionBatchDAO.getBetween(-1L,
                queryFor3daysLater[0],
                queryFor3daysLater[1]);

        final ArrayList<TrackerMotion> actualEmpty = new ArrayList<>();
        for(final TrackerMotion.Batch batch:emptyBatches){
            for(final TrackerMotion trackerMotion:batch.motionData){
                actualEmpty.add(trackerMotion);
            }
        }

        final List<TrackerMotion> expectedEmpty = Collections.<TrackerMotion>emptyList();

        assertThat(expectedEmpty, containsInAnyOrder(actualEmpty.toArray()));
    }


    @Test(expected = UnableToExecuteStatementException.class)
    public void testDuplicationInsertException(){
        final DateTime startDateTime = DateTime.now();
        final ArrayList<TrackerMotion> motions = new ArrayList<>();

        // Databatch 1
        final TrackerMotion trackerMotion = new TrackerMotion(-1, startDateTime.getMillis(), 100, startDateTime.getZone().getOffset(startDateTime));
        motions.add(trackerMotion);

        // Duplicated batch
        final TrackerMotion.Batch batch1 = new TrackerMotion.Batch(-1, startDateTime.getMillis(), startDateTime.getZone().getOffset(startDateTime), motions);
        this.trackerMotionBatchDAO.insert(batch1);

        final TrackerMotion.Batch duplicatedBatch = new TrackerMotion.Batch(-1, startDateTime.getMillis(), startDateTime.getZone().getOffset(startDateTime), motions);
        this.trackerMotionBatchDAO.insert(duplicatedBatch);
    }

}

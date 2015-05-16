package com.hello.suripu.core.util;

import com.hello.suripu.core.models.TrackerMotion;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 12/8/14.
 */
public class SleepDepthTest {
    @Test
    public void testPercentagePositionSleepDepth(){
        final File csvFile = new File(FileUtils.getResourceFilePath("fixtures/algorithm/pill_data_12_06_2014_caroline.csv"));

        final TimelineUtils timelineUtils = new TimelineUtils();

        try {
            final BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFile));
            String line = bufferedReader.readLine();

            final List<TrackerMotion> trackerMotionList = new ArrayList<>();
            long ts = 0;
            while ((line = bufferedReader.readLine()) != null) {
                // process the line.
                final String[] columns = line.split(",");
                trackerMotionList.add(new TrackerMotion(Integer.valueOf(columns[0]),
                        0L,
                        1L,
                        ts++,
                        Integer.valueOf(columns[1]),
                        0,
                        0L, 0L,0L));

            }
            bufferedReader.close();

            final Map<Integer, Integer> valuePositions = timelineUtils.constructValuePositionMap(trackerMotionList);
            assertThat(valuePositions.get(2940), is(100 - 89));
        }catch (Exception ex){
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }


    @Test
    public void testGetSleepDepthWithSmallData(){
        final Integer[] motionAmplitudes = new Integer[]{1, 20, 100};
        final List<TrackerMotion> trackerMotionList = new ArrayList<>();
        int ts = 0;

        for(final Integer amplitude:motionAmplitudes){
            trackerMotionList.add(new TrackerMotion(ts,
                    0L,
                    1L,
                    ts++,
                    amplitude,
                    0,0L, 0L,0L));
        }

        final TimelineUtils timelineUtils = new TimelineUtils();

        assertThat(timelineUtils.getSleepDepth(70, timelineUtils.constructValuePositionMap(trackerMotionList), 100), is(100-70));
    }
}

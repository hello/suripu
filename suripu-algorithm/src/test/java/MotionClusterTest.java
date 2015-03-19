import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.sleep.MotionCluster;
import com.hello.suripu.algorithm.utils.ClusterAmplitudeData;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/19/15.
 */
public class MotionClusterTest {

    public List<AmplitudeData> loadFromResource(final String path){
        final URL fixtureCSVFile = Resources.getResource(path);
        final List<AmplitudeData> data = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final AmplitudeData datum = new AmplitudeData(Long.valueOf(columns[0]), Double.valueOf(columns[1]), Integer.valueOf(columns[2]));
                data.add(datum);
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
        return ImmutableList.copyOf(data);
    }

    public List<ClusterAmplitudeData> loadClustersFromResource(final String path){
        final URL fixtureCSVFile = Resources.getResource(path);
        final List<ClusterAmplitudeData> data = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final AmplitudeData amplitudeData = new AmplitudeData(Long.valueOf(columns[0]), Double.valueOf(columns[1]), Integer.valueOf(columns[2]));
                final ClusterAmplitudeData clusterAmplitudeData = ClusterAmplitudeData.create(amplitudeData, Boolean.parseBoolean(columns[3]));
                data.add(clusterAmplitudeData);
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
        return ImmutableList.copyOf(data);
    }

    @Test
    public void testGetClusters(){
        final List<AmplitudeData> input = loadFromResource("fixtures/km_motion_2015_03_15_gap_filled.csv");
        final List<ClusterAmplitudeData> expected = loadClustersFromResource("fixtures/km_motion_2015_03_15_clustered.csv");
        final List<ClusterAmplitudeData> actual = MotionCluster.getClusters(input, MotionCluster.DEFAULT_STD_COUNT, 6);

        assertThat(expected.size(), is(actual.size()));
        for(int i = 0; i < actual.size(); i++){
            assertThat(expected.get(i), is(actual.get(i)));
        }
    }
}

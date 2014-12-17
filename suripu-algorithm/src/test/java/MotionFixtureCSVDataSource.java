import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 12/14/14.
 */
public class MotionFixtureCSVDataSource implements DataSource<AmplitudeData> {

    private final List<AmplitudeData> data = new LinkedList<>();
    public MotionFixtureCSVDataSource(final String fixtureFilePath){

        final URL fixtureCSVFile = Resources.getResource(fixtureFilePath);

        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                this.data.add(new AmplitudeData(Long.valueOf(columns[0]), Double.valueOf(columns[1]), Integer.valueOf(columns[2])));
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    @Override
    public ImmutableList<AmplitudeData> getDataForDate(final DateTime day) {
        return ImmutableList.copyOf(this.data);
    }
}

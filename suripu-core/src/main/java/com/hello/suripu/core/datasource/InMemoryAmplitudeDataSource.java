package com.hello.suripu.core.datasource;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 6/16/14.
 */
public class InMemoryAmplitudeDataSource implements DataSource<AmplitudeData> {

    private final List<AmplitudeData> fullData;

    public InMemoryAmplitudeDataSource(final List<AmplitudeData> fullData){
        this.fullData = fullData;
    }

    @Override
    public ImmutableList<AmplitudeData> getDataForDate(final DateTime day) {
        final LinkedList<AmplitudeData> selectedData = new LinkedList<AmplitudeData>();
        final DateTime targetDateStart = day.withTimeAtStartOfDay().plusHours(12);
        final DateTime targetDateEnd = targetDateStart.plusDays(1);

        for(int i = 0; i < this.fullData.size(); i++){
            final AmplitudeData datum = this.fullData.get(i);
            if(datum.timestamp >= targetDateStart.getMillis() && datum.timestamp <= targetDateEnd.getMillis()){
                selectedData.add(datum);
            }
        }

        return ImmutableList.copyOf(selectedData);
    }
}

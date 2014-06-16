package com.hello.suripu.algorithm.event;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 6/16/14.
 */
public class EventDetectionAlgorithm {

    private final DataSource<AmplitudeData> dataSource;
    private final int meanCrossBufferWindowMillis;

    public EventDetectionAlgorithm(final DataSource<AmplitudeData> dataSource,
                                   final int meanCrossBufferWindowMillis){
        this.dataSource = dataSource;
        this.meanCrossBufferWindowMillis = meanCrossBufferWindowMillis;
    }

    private Optional<Segment> isEvent(final AmplitudeData current, final double mean){
        if(mean >= 0){
            if(current.amplitude > mean * 2){
                final Segment event = new Segment();
                event.setStartTimestamp(current.timestamp);
                event.setEndTimestamp(current.timestamp + 60 * 1000);
                event.setOffsetMillis(current.offsetMillis);

                return Optional.of(event);
            }else{
                return Optional.absent();
            }
        }else{
            if(current.amplitude > mean / 2d){
                final Segment event = new Segment();
                event.setStartTimestamp(current.timestamp);
                event.setEndTimestamp(current.timestamp + 60 * 1000);
                event.setOffsetMillis(current.offsetMillis);

                return Optional.of(event);
            }else{
                return Optional.absent();
            }
        }
    }

    public ImmutableList<Segment> getEventsForDate(final DateTime targetDate){
        final List<AmplitudeData> rawData = getDataSource().getDataForDate(targetDate);
        final LinkedList<AmplitudeData> buffer = new LinkedList<AmplitudeData>();
        final LinkedList<Segment> events = new LinkedList<Segment>();

        for(int i = 0; i < rawData.size(); i++){
            if(buffer.size() == 0){
                buffer.add(rawData.get(i));
            }else{
                final double bufferMean = NumericalUtils.selectAverage(buffer);
                final AmplitudeData current = rawData.get(i);

                if(buffer.getLast().timestamp - buffer.getFirst().timestamp < getMeanCrossBufferWindowMillis()){
                    buffer.add(current);
                }else{
                    buffer.removeFirst();
                    buffer.add(current);
                }

                final Optional<Segment> segmentOptional = isEvent(current, bufferMean);
                if(segmentOptional.isPresent()){
                    events.add(segmentOptional.get());
                }
            }
        }

        return ImmutableList.copyOf(events);
    }


    protected DataSource<AmplitudeData> getDataSource(){
        return this.dataSource;
    }

    protected int getMeanCrossBufferWindowMillis(){
        return this.meanCrossBufferWindowMillis;
    }

}

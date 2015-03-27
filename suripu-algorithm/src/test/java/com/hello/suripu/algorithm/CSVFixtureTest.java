package com.hello.suripu.algorithm;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.sleep.ClusterAmplitudeData;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 3/23/15.
 */
public class CSVFixtureTest {
    public List<AmplitudeData> loadAmpFromResource(final String path){
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

    public List<AmplitudeData> loadKickOffFromResource(final String path){
        final URL fixtureCSVFile = Resources.getResource(path);
        final List<AmplitudeData> data = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                final AmplitudeData datum = new AmplitudeData(Long.valueOf(columns[0]), Double.valueOf(columns[3]), Integer.valueOf(columns[2]));
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
}

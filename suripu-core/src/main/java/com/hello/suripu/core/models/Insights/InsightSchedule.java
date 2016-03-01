package com.hello.suripu.core.models.Insights;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Created by jyfan on 2/19/16.
 */
public class InsightSchedule {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightSchedule.class);

    public enum InsightGroup {
        DEFAULT(0),
        CBTI_V1(1);

        private int value;
        InsightGroup(final int value) {this.value = value;}

        public int getValue() {return this.value;}

        public String toInsightGroupString() {
            return String.format("%03d", this.getValue());
        }

        public static InsightGroup fromInteger(final int value) {
            for (final InsightGroup group : InsightGroup.values()) {
                if (value == group.getValue())
                    return group;
            }
            return InsightGroup.DEFAULT;
        }

        public static InsightGroup fromString(final String text) {
            if (text != null) {
                for (final InsightGroup group : InsightGroup.values()) {
                    if (text.equalsIgnoreCase(group.toString()))
                        return group;
                }
            }
            throw new IllegalArgumentException("Invalid InsightGroup string");
        }
    }

    @JsonProperty("group")
    public final InsightGroup group;

    @JsonProperty("year")
    public final Integer year;

    @JsonProperty("month")
    public final Integer month;

    @JsonProperty("map")
    public final Map<Integer, InsightCard.Category> dayToCategoryMap;

    private InsightSchedule(final InsightGroup group, final Integer year, final Integer month) {
        this(group, year, month, Collections.<Integer,InsightCard.Category>emptyMap());
    }

    private InsightSchedule(final InsightGroup group, final Integer year, final Integer month, final Map<Integer, InsightCard.Category> dayToCategoryMap) {
        this.group = group;
        this.year = year;
        this.month = month;
        this.dayToCategoryMap = dayToCategoryMap;
    }

    public static InsightSchedule loadInsightSchedule(final AmazonS3Client amazons3client, final String insightScheduleBucket, final InsightGroup insightGroup, final Integer year, final Integer month) {

        final String bucket = insightScheduleBucket;
        final String key = String.format("insight_schedule_%d-%d_%s.yml", year, month, insightGroup).toLowerCase();

        try {
            final S3Object s3Object = amazons3client.getObject(bucket, key);
            try (final Reader inputStream = new InputStreamReader(s3Object.getObjectContent())) {
                final String base64data = CharStreams.toString(inputStream);
                final Map<Integer, InsightCard.Category> dayToCategoryMap = new ObjectMapper(new YAMLFactory()).readValue(base64data, new TypeReference<Map<Integer, InsightCard.Category>>() {});
                final InsightSchedule insightSchedule = new InsightSchedule(insightGroup, year, month, dayToCategoryMap);
                return insightSchedule;
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.debug(e.getMessage());
                final InsightSchedule insightScheduleEmpty = new InsightSchedule(insightGroup, year, month);
                return insightScheduleEmpty;
            }
        } catch (AmazonClientException ace) {
            LOGGER.debug(ace.getMessage());
            final InsightSchedule insightScheduleEmpty = new InsightSchedule(insightGroup, year, month);
            return insightScheduleEmpty;
        }
    }
}

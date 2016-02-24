package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    public static InsightSchedule loadInsightSchedule(final InsightGroup insightGroup, final Integer year, final Integer month) {

        final String resourceString = String.format("insights/insight_schedule_%d-%d_%s.json", year, month, insightGroup).toLowerCase();

        try {
            final URL insightScheduleJSONFileValid = Resources.getResource(resourceString);
            final Map<Integer, InsightCard.Category> dayToCategoryMap = new ObjectMapper().readValue(insightScheduleJSONFileValid, new TypeReference<Map<Integer, InsightCard.Category>>() {
            });
            final InsightSchedule insightSchedule = new InsightSchedule(insightGroup, year, month, dayToCategoryMap);
            return insightSchedule;
        }
        catch (IllegalArgumentException | IOException e) {
            LOGGER.debug(e.getMessage());
            final InsightSchedule insightScheduleEmpty = new InsightSchedule(insightGroup, year, month);
            return insightScheduleEmpty;
        }
    }
}

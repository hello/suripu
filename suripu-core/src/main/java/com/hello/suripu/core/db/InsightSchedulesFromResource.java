package com.hello.suripu.core.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.InsightSchedule;
import com.hello.suripu.core.models.Insights.InsightScheduleMap;
import com.hello.suripu.core.models.Insights.InsightScheduleYear;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Created by jyfan on 3/4/16.
 */
public class InsightSchedulesFromResource implements InsightScheduleDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightSchedulesFromResource.class);

    private final InsightSchedule insightScheduleCBTI_v1;
    private final InsightSchedule insightScheduleDefault;

    private static InsightSchedule getInsightScheduleFromResource(final String insightScheduleLocation, final InsightSchedule.InsightGroup insightGroup, final Integer year, final Integer month) {
        try {
            final String resourcePath = insightScheduleLocation + "/" + InsightSchedule.getScheduleFileName(year, month, insightGroup);
            final URL insightScheduleYAMLFileValid = Resources.getResource(resourcePath);

            final Map<Integer, Map<Integer, Map<Integer, InsightCard.Category>>> map = new ObjectMapper(new YAMLFactory()).readValue( insightScheduleYAMLFileValid, new TypeReference<Map<Integer, Map<Integer, Map<Integer, InsightCard.Category>>>>(){} );
            final InsightSchedule insightSchedule = new InsightSchedule(insightGroup, map);
            return insightSchedule;
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error(e.getMessage());
            final InsightSchedule insightScheduleEmpty = new InsightSchedule(insightGroup);
            return insightScheduleEmpty;
        }
    }

    public static InsightScheduleDAO create(final String insightScheduleBucket, final Integer year, final Integer month) {
        final InsightSchedule insightScheduleCBTI_v1 = getInsightScheduleFromResource(insightScheduleBucket, InsightSchedule.InsightGroup.CBTI_V1, year, month);
        final InsightSchedule insightScheduleDeafult = getInsightScheduleFromResource(insightScheduleBucket, InsightSchedule.InsightGroup.DEFAULT, year, month);
        return new InsightSchedulesFromResource(insightScheduleDeafult, insightScheduleCBTI_v1);
    }

    private InsightSchedulesFromResource(InsightSchedule insightScheduleDefault, InsightSchedule insightScheduleCBTI_v1) {
        this.insightScheduleDefault = insightScheduleDefault;
        this.insightScheduleCBTI_v1 = insightScheduleCBTI_v1;
    }

    @Override
    public InsightSchedule getInsightScheduleCBTI_V1() {return insightScheduleCBTI_v1;}

    @Override
    public InsightSchedule getInsightScheduleDefault() {return insightScheduleDefault;}

}

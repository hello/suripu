package com.hello.suripu.core.trends.v2;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTimeConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 1/22/16.
 */

public class TrendsProcessorUtils {

    public static class AnnotationStats {
        public float sumValues;
        public float sumWeekdayValues;
        public float numWeekdays;
        public float sumWeekendValues;
        public float numWeekends;
        public float numDays;

        public AnnotationStats() {
            this.sumValues = 0.0f;
            this.sumWeekdayValues = 0.0f;
            this.numWeekdays = 0.0f;
            this.sumWeekendValues = 0.0f;
            this.numWeekends = 0.0f;
            this.numDays = 0.0f;
        }
    }

    public final static Map<Integer, String> DOW_DAY_MAP;
    static {
        final Map<Integer, String> dowMap = Maps.newLinkedHashMap();
        dowMap.put(7, "SUN");
        dowMap.put(1, "MON");
        dowMap.put(2, "TUE");
        dowMap.put(3, "WED");
        dowMap.put(4, "THU");
        dowMap.put(5, "FRI");
        dowMap.put(6, "SAT");
        DOW_DAY_MAP = ImmutableMap.copyOf(dowMap);
    }



    public static Optional<List<Annotation>> getAnnotations(final AnnotationStats stats, final DataType dataType) {
        final List<Annotation> annotations = Lists.newArrayList();
        if (stats.numDays > 0.0f) { // Average
            String title = "AVERAGE SCORE";
            if (dataType.equals(DataType.HOURS)) {
                title = "TOTAL AVERAGE";
            }
            annotations.add(new Annotation(title, stats.sumValues/stats.numDays, dataType, Optional.<Condition>absent()));
        }

        if (stats.numWeekdays > 0.0f) {
            annotations.add(new Annotation("WEEKDAYS", stats.sumWeekdayValues/stats.numWeekdays, dataType, Optional.<Condition>absent()));
        }

        if (stats.numWeekends > 0.0f) {
            annotations.add(new Annotation("WEEKENDS", stats.sumWeekendValues/stats.numWeekends, dataType, Optional.<Condition>absent()));
        }

        if (!annotations.isEmpty()) {
            return Optional.of(annotations);
        }

        return Optional.absent();
    }

    public static List<GraphSection> getSections(final List<Float> dayOfWeekData, final DataType dataType, final TimeScale timeScale, final int today) {
        if (dataType.equals(DataType.SCORES) && (!timeScale.equals(TimeScale.LAST_THREE_MONTHS))) {
            return getScoreWeekSections(dayOfWeekData, timeScale, today);
        }
        return Collections.<GraphSection>emptyList();
    }

    private static List<GraphSection> getScoreWeekSections(final List<Float>  dayOfWeekData, final TimeScale timeScale, final int today) {
        final List<GraphSection> sections = Lists.newArrayList();
        final List<String> title = Lists.newArrayList(DOW_DAY_MAP.values());
        final int numWeeks = dayOfWeekData.size() / DateTimeConstants.DAYS_PER_WEEK;

        int weeks = 0;
        for (final List<Float> oneWeek : Lists.partition(dayOfWeekData, DateTimeConstants.DAYS_PER_WEEK)) {
            weeks++;
            if (weeks == 1) {
                sections.add(new GraphSection(oneWeek, Optional.of(title), Collections.<Integer>emptyList(), Optional.of(today)));
            } else {
                final List<Integer> highlightedValues = Lists.newArrayList();
                if (weeks == numWeeks) {
                    if (today > 0) {
                        highlightedValues.add(today - 1); // today should always be > 0
                    }
                }
                sections.add(new GraphSection(oneWeek, Optional.<List<String>>absent(), highlightedValues, Optional.<Integer>absent()));
            }

        }

        return sections;
    }
}

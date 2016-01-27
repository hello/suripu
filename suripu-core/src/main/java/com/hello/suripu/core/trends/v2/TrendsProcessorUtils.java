package com.hello.suripu.core.trends.v2;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;

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

    public final static String getMonthName(int month){
        String[] monthNames = {"NON", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        return monthNames[month];
    }

    public static Optional<List<Annotation>> getAnnotations(final AnnotationStats stats, final DataType dataType) {
        final List<Annotation> annotations = Lists.newArrayList();
        if (stats.numDays > 0.0f) { // Average
            String title = "TOTAL AVERAGE";
            final float avg = stats.sumValues/stats.numDays;
            Optional<Condition> condition = Optional.<Condition>absent();
            if (dataType.equals(DataType.SCORES)) {
                title = "AVERAGE SCORE";
                condition = Optional.of(Condition.getScoreCondition(avg));
            }

            annotations.add(new Annotation(title, avg, dataType, condition));
        }

        if (stats.numWeekdays > 0.0f) {
            final float avg = stats.sumWeekdayValues/stats.numWeekdays;
            Optional<Condition> condition = Optional.<Condition>absent();
            if (dataType.equals(DataType.SCORES)) {
                condition = Optional.of(Condition.getScoreCondition(avg));
            }
            annotations.add(new Annotation("WEEKDAYS", avg, dataType, condition));
        }

        if (stats.numWeekends > 0.0f) {
            final float avg = stats.sumWeekendValues/stats.numWeekends;
            Optional<Condition> condition = Optional.<Condition>absent();
            if (dataType.equals(DataType.SCORES)) {
                condition = Optional.of(Condition.getScoreCondition(avg));
            }
            annotations.add(new Annotation("WEEKENDS", avg, dataType, condition));
        }

        if (!annotations.isEmpty()) {
            return Optional.of(annotations);
        }

        return Optional.absent();
    }

    public static List<GraphSection> getSections(final List<Float> data,
                                                 final float minValue, final float maxValue,
                                                 final DataType dataType,
                                                 final TimeScale timeScale,
                                                 final DateTime today) {

        if (dataType.equals(DataType.SCORES)) {
            // sleep score
            if (!timeScale.equals(TimeScale.LAST_THREE_MONTHS)) {
                return getScoreWeekSections(data, today); // last_week & last_month
            }

            // last_3_months
            return getScoreMonthSections(data, today);

        } else if (dataType.equals(DataType.HOURS)) {
            // sleep duration
            if (timeScale.equals(TimeScale.LAST_WEEK)) {
                return getDurationWeekSection(data, minValue, maxValue, today); // last_week
            }

            // last_month & last_3_months
            return getDurationMonthSections(data, minValue, maxValue, timeScale, today);

        } else {
            // sleep depth bubbles
        }

        return Collections.<GraphSection>emptyList();
    }

    private static List<GraphSection> getScoreWeekSections(final List<Float> dayOfWeekData, final DateTime today) {
        final List<GraphSection> sections = Lists.newArrayList();
        final List<String> title = Lists.newArrayList(DOW_DAY_MAP.values());
        final int numWeeks = dayOfWeekData.size() / DateTimeConstants.DAYS_PER_WEEK;

        final int todayDOW = today.getDayOfWeek();
        int weeks = 0;
        for (final List<Float> oneWeek : Lists.partition(dayOfWeekData, DateTimeConstants.DAYS_PER_WEEK)) {
            weeks++;
            if (weeks == 1) {
                sections.add(new GraphSection(oneWeek, Optional.of(title), Collections.<Integer>emptyList(), Optional.of(todayDOW)));
            } else {
                final List<Integer> highlightedValues = Lists.newArrayList();
                if (weeks == numWeeks) {
                    if (todayDOW > 0) {
                        highlightedValues.add(todayDOW - 1); // today should always be > 0
                    }
                }
                sections.add(new GraphSection(oneWeek, Optional.<List<String>>absent(), highlightedValues, Optional.<Integer>absent()));
            }

        }
        return sections;
    }

    private static List<GraphSection> getScoreMonthSections(final List<Float> dayOfWeekData, final DateTime today) {
        // TODO
        return Collections.<GraphSection>emptyList();
    }

    private static List<GraphSection> getDurationWeekSection(final List<Float> data, final float minValue, final float maxValue, final DateTime today) {

        // populate titles
        final List<String> title = Lists.newArrayList();
        for (int day = 1; day <= DateTimeConstants.DAYS_PER_WEEK; day++) {
            final int dayOfWeek = today.minusDays(day).getDayOfWeek();
            title.add(DOW_DAY_MAP.get(dayOfWeek));
        }

        // populate values
        final List<Float> sectionValues = Lists.newArrayList();
        final List<Integer> highlights = Lists.newArrayList();
        int index = 0;
        for(final Float value: data) {
            if (value == null) {
                continue;
            }

            // highlight these values
            if (value == minValue || value == maxValue) {
                highlights.add(index);
            }

            sectionValues.add(value);
            index++;
        }

        return Lists.newArrayList(
                new GraphSection(sectionValues,
                        Optional.of(title),
                        highlights,
                        Optional.of(DateTimeConstants.DAYS_PER_WEEK - 1)) // highlight title is always yesterday, last element
        );

    }

    private static List<GraphSection> getDurationMonthSections(final List<Float> data,
                                                               final float minValue, final float maxValue,
                                                               final TimeScale timeScale,
                                                               final DateTime today) {

        // compile values
        int index = 0;
        final List<Float> sectionValues = Lists.newArrayList();
        int minHighlightIndex = -1;
        int maxHighlightIndex = -1;
        for(final Float value: data) {
            if (value == null) {
                continue;
            }

            // highlight these values
            if (value == minValue) {
                minHighlightIndex = index;
            } else if (maxHighlightIndex == -1 && value == maxValue) {
                maxHighlightIndex = index;
            }

            index++;
            sectionValues.add(value);
        }

        final DateTime firstDate = today.minusDays(timeScale.getDays());
        String title = getMonthName(firstDate.getMonthOfYear());
        final List<GraphSection> sections = Lists.newArrayList();
        int sectionFirstIndex = 0;

        for (int day = 0; day < timeScale.getDays(); day ++) {
            final String currentMonth = getMonthName(firstDate.plusDays(day).getMonthOfYear());
            if (!title.equals(currentMonth)) {

                final List<Integer> highlightValues = Lists.newArrayList();
                if (minHighlightIndex > 0 && minHighlightIndex < day) {
                    highlightValues.add(minHighlightIndex);
                    minHighlightIndex = -1; // reset
                }

                if (maxHighlightIndex > 0 && maxHighlightIndex < day) {
                    highlightValues.add(maxHighlightIndex);
                    maxHighlightIndex = -1;
                }

                sections.add(new GraphSection(sectionValues.subList(sectionFirstIndex, day),
                                Optional.<List<String>>of(Lists.newArrayList(title)),
                                highlightValues,
                                Optional.<Integer>absent())
                );
                sectionFirstIndex = day;
                title = currentMonth;
            }
        }

        final int lastDay = timeScale.getDays() - 1;
        if (sectionFirstIndex != lastDay) {

            final List<Integer> highlightValues = Lists.newArrayList();
            if (minHighlightIndex > 0 && minHighlightIndex <= lastDay) {
                highlightValues.add(minHighlightIndex);
            }

            if (maxHighlightIndex > 0 && maxHighlightIndex <= lastDay) {
                highlightValues.add(maxHighlightIndex);
            }

            sections.add(new GraphSection(sectionValues.subList(sectionFirstIndex, lastDay + 1),
                            Optional.<List<String>>of(Lists.newArrayList(title)),
                            highlightValues,
                            Optional.<Integer>absent())
            );
        }

        return sections;
    }

    public static List<Float> padSectionData(final List<Float> data, final DateTime today, final DateTime firstDataDateTime, final DateTime lastDataDateTime, final int numDays) {
        final List<Float> sectionData = Lists.newArrayList();

        // fill in missing days first
        final DateTime firstDate = today.minusDays(numDays);
        final Days missingDays = Days.daysBetween(firstDate, firstDataDateTime);
        if (missingDays.getDays() > 0) {
            for (int day = 0; day < missingDays.getDays(); day ++) {
                sectionData.add(GraphSection.MISSING_VALUE);
            }
        }

        // pad front with nulls
        final int firstDateDOW = firstDate.getDayOfWeek();
        if (firstDateDOW < DateTimeConstants.SUNDAY) {
            for (int day = 0; day < firstDateDOW; day++) {
                sectionData.add(0, null);
            }
        }

        sectionData.addAll(data);

        // add missing values at the end
        final int lastDataDOW = lastDataDateTime.getDayOfWeek();

        final Days endMissingDays = Days.daysBetween(lastDataDateTime, today.minusDays(1));
        if (endMissingDays.getDays() > 0) {
            for (int day = 0; day < endMissingDays.getDays(); day ++) {
                sectionData.add(GraphSection.MISSING_VALUE);
            }
        }

        // pad ends with nulls
        final int todayDOW = today.getDayOfWeek();
        if (todayDOW < DateTimeConstants.SUNDAY) {
            for (int day = todayDOW; day < DateTimeConstants.SUNDAY; day++) {
                sectionData.add(null);
            }
        }
        return sectionData;
    }
}

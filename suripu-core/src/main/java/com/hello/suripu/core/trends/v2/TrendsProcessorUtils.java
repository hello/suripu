package com.hello.suripu.core.trends.v2;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.Months;

import java.util.Collections;
import java.util.List;

/**
 * Created by ksg on 01/22/2016
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

    public static List<Annotation> getAnnotations(final AnnotationStats stats, final DataType dataType) {

        final List<Annotation> annotations = Lists.newArrayList();

        if (stats.numWeekdays > 0.0f) {
            final float avg = Math.round((stats.sumWeekdayValues/stats.numWeekdays) * 10.0f) / 10.0f ; // to make single decimal
            Optional<Condition> condition = Optional.absent();
            if (dataType.equals(DataType.SCORES)) {
                condition = Optional.of(Condition.getScoreCondition(avg));
            }
            annotations.add(new Annotation(English.ANNOTATION_WEEKDAYS, avg, dataType, condition));
        }

        if (stats.numWeekends > 0.0f) {
            final float avg = Math.round((stats.sumWeekendValues/stats.numWeekends) * 10.0f) / 10.0f;
            Optional<Condition> condition = Optional.absent();
            if (dataType.equals(DataType.SCORES)) {
                condition = Optional.of(Condition.getScoreCondition(avg));
            }
            annotations.add(new Annotation(English.ANNOTATION_WEEKENDS, avg, dataType, condition));
        }

        if (stats.numDays > 0.0f) { // Average
            final float avg = Math.round((stats.sumValues/stats.numDays) * 10.0f) / 10.0f;
            String title = English.ANNOTATION_AVERAGE;
            Optional<Condition> condition = Optional.absent();
            if (dataType.equals(DataType.SCORES)) {
                condition = Optional.of(Condition.getScoreCondition(avg));
            }

            annotations.add(new Annotation(title, avg, dataType, condition));
        }

        if (annotations.size() < 3) {
            return Collections.emptyList();
        }

        return annotations;
    }


    public static List<GraphSection> getScoreDurationSections(final List<Float> data,
                                                 final float minValue, final float maxValue,
                                                 final DataType dataType,
                                                 final TimeScale timeScale,
                                                 final DateTime today,
                                                 final boolean hasMinMaxValues) {
        if (dataType.equals(DataType.SCORES)) {
            if (!timeScale.equals(TimeScale.LAST_3_MONTHS)) {
                return getScoreWeekSections(data, today); // last_week & last_month
            }
            return getScoreThreeMonthsSections(data, timeScale, today); // special-case: last_3_months

        }

        // sleep duration
        if (timeScale.equals(TimeScale.LAST_WEEK)) {
            return getDurationWeekSection(data, minValue, maxValue, today, hasMinMaxValues); // special-case: last_week
        }
        // last_month & last_3_months
        return getDurationMonthSections(data, minValue, maxValue, timeScale, today, hasMinMaxValues);
    }


    private static List<GraphSection> getScoreWeekSections(final List<Float> data, final DateTime today) {
        final List<GraphSection> sections = Lists.newArrayList();
        final int numWeeks = data.size() / DateTimeConstants.DAYS_PER_WEEK;

        final int todayDOW = today.getDayOfWeek();
        int weeks = 0;
        for (final List<Float> oneWeek : Lists.partition(data, DateTimeConstants.DAYS_PER_WEEK)) {
            weeks++;
            Optional<Integer> highlightedTitle = Optional.<Integer>absent();
            List<String> titles = Collections.<String>emptyList();
            if (weeks == 1) {
                highlightedTitle = Optional.of(todayDOW - 1);
                titles = English.DAY_OF_WEEK_NAMES;
                if (numWeeks > 1) {
                    sections.add(new GraphSection(oneWeek, titles, Collections.<Integer>emptyList(), highlightedTitle));
                    continue;
                }
            }

            final List<Integer> highlightedValues = Lists.newArrayList();
            if (weeks == numWeeks) {
                if (todayDOW > 0) {
                    highlightedValues.add(todayDOW - 1); // today should always be > 0
                }
            }
            // no titles for subsequent sections
            sections.add(new GraphSection(oneWeek, titles, highlightedValues, highlightedTitle));

        }
        return sections;
    }

    private static List<GraphSection> getDurationWeekSection(final List<Float> data, final float minValue, final float maxValue, final DateTime today, final boolean hasMinMaxValues) {

        // populate titles
        final List<String> title = Lists.newArrayList();
        for (int day = DateTimeConstants.DAYS_PER_WEEK; day >= 1; day--) {
            final int dayOfWeek = today.minusDays(day).getDayOfWeek();
            final int dayOfWeekIndex = (dayOfWeek == DateTimeConstants.SUNDAY) ? 0 : dayOfWeek;
            title.add(English.DAY_OF_WEEK_NAMES.get(dayOfWeekIndex));
        }

        // populate values
        final List<Float> sectionValues = Lists.newArrayList();
        final List<Integer> highlights = Lists.newArrayList();
        int minIndex = -1;
        int maxIndex = -1;
        int index = 0;
        for(final Float value: data) {
            if (value == null) {
                continue;
            }

            // highlight these values
            if (hasMinMaxValues) {
                if (value == minValue) {
                    minIndex = index;
                } else if (maxIndex == -1 && value == maxValue) {
                    maxIndex = index;
                }
            }

            sectionValues.add(value);
            index++;
        }

        // pad front section to make it 7 days
        final int daysDiff = DateTimeConstants.DAYS_PER_WEEK - sectionValues.size();
        if (daysDiff > 0) {
            for (int i = 0; i < daysDiff; i++) {
                sectionValues.add(0, GraphSection.MISSING_VALUE);

                if (minIndex >= 0) { // adjust
                    minIndex++;
                }

                if (maxIndex >= 0) {
                    maxIndex++;
                }
            }
        }

        if (maxIndex == minIndex) {
            minIndex = -1;
        }

        if (minIndex >= 0) {
            highlights.add(minIndex);
        }

        if (maxIndex >= 0) {
            highlights.add(maxIndex);
        }

        return Lists.newArrayList(
                new GraphSection(sectionValues,
                        title,
                        highlights,
                        Optional.of(DateTimeConstants.DAYS_PER_WEEK - 1)) // highlight title is always yesterday, last element
        );

    }

    private static List<GraphSection> getDurationMonthSections(final List<Float> data,
                                                               final float minValue, final float maxValue,
                                                               final TimeScale timeScale,
                                                               final DateTime today,
                                                               final boolean hasMinMaxValues) {
        // compile values
        int index = 0;
        int minIndex = -1;
        int maxIndex = -1;
        final List<Float> sectionValues = Lists.newArrayList();
        for(final Float value: data) {
            if (value != null) {
                if (value == minValue) {
                    minIndex = index; // highlight last min value
                } else if (maxIndex == -1 && value == maxValue) {
                    maxIndex = index; // highlight first max
                }

                sectionValues.add(value);
            } else {
                sectionValues.add(GraphSection.MISSING_VALUE);
            }
            index++;

        }

        if (minIndex == maxIndex) {
            minIndex = -1; // just in case
        }

        // create the sections
        final List<GraphSection> sections = Lists.newArrayList();

        final DateTime firstDate = today.minusDays(timeScale.getDays());
        String title = English.MONTH_OF_YEAR_NAMES.get(firstDate.getMonthOfYear() - 1);
        int sectionFirstIndex = 0;
        int added = 0;

        for (int day = 0; day < timeScale.getDays(); day ++) {
            final String monthName = English.MONTH_OF_YEAR_NAMES.get(firstDate.plusDays(day).getMonthOfYear() - 1);
            if (!title.equals(monthName)) {

                final List<Integer> highlightValues = Lists.newArrayList();
                if (hasMinMaxValues) {
                    if (minIndex >= 0 && minIndex < day) {
                        highlightValues.add(minIndex - sectionFirstIndex); // position within current section
                        minIndex = -1; // reset
                    }

                    if (maxIndex >= 0 && maxIndex < day) {
                        highlightValues.add(maxIndex - sectionFirstIndex);
                        maxIndex = -1;
                    }
                }

                sections.add(new GraphSection(
                                sectionValues.subList(sectionFirstIndex, day),
                                Lists.newArrayList(title),
                                highlightValues,
                                Optional.<Integer>absent())
                );
                added += (day - sectionFirstIndex);
                sectionFirstIndex = day;
                title = monthName;
            }
        }

        // add remaining data
        final int lastDay = timeScale.getDays() - 1;
        if (added < sectionValues.size()) {
            final List<Integer> highlightValues = Lists.newArrayList();
            if (hasMinMaxValues) {
                if (minIndex >= 0 && minIndex <= lastDay) {
                    highlightValues.add(minIndex - sectionFirstIndex);
                }

                if (maxIndex >= 0 && maxIndex <= lastDay) {
                    highlightValues.add(maxIndex - sectionFirstIndex);
                }
            }

            sections.add(new GraphSection(
                            sectionValues.subList(sectionFirstIndex, lastDay + 1),
                            Lists.newArrayList(title),
                            highlightValues,
                            Optional.<Integer>absent())
            );
        }

        return sections;
    }

    private static List<GraphSection> getScoreThreeMonthsSections(final List<Float> data, final TimeScale timeScale, final DateTime today) {
        final List<Float> sectionData = Lists.newArrayList();

        // pad first month starting from the 1st
        final DateTime firstDate = today.minusDays(timeScale.getDays());
        if (firstDate.getDayOfMonth() != 1) {
            for (int day = 0; day < firstDate.getDayOfMonth() - 1; day++) { // firstDate already padded
                sectionData.add(null);
            }
        }

        sectionData.addAll(data);

        // pad current month till the last day
        final DateTime lastDate = today.dayOfMonth().withMaximumValue();
        if (today.getDayOfMonth() > 1 && today.getDayOfMonth() <= lastDate.getDayOfMonth()) {
            final int numExtraDays = lastDate.getDayOfMonth() - today.getDayOfMonth();
            for (int day = 0; day <= numExtraDays; day++) {
                sectionData.add(null);
            }
        }

        final List<GraphSection> sections = Lists.newArrayList();

        final int numMonthsInData = Months.monthsBetween(firstDate, lastDate).getMonths() + 1;
        int sectionFirstIndex = 0;
        boolean highlighted = false;

        for (int month = 0; month < numMonthsInData; month++) {
            final DateTime currentMonth = firstDate.plusMonths(month);

            List<Integer> highlightedValues = Lists.newArrayList();
            Optional<Integer> highlightTitle = Optional.absent();

            if (!highlighted && (month == (numMonthsInData - 1) ||
                    (today.getDayOfMonth() == 1 && month == (numMonthsInData-2)))) {
                // highlights only appear in the last month
                final int yesterday = today.minusDays(1).getDayOfMonth();
                highlightedValues.add(yesterday - 1);
                highlightTitle = Optional.of(0);
                highlighted = true;
            }

            final String title = English.MONTH_OF_YEAR_NAMES.get(currentMonth.getMonthOfYear() - 1);
            final int maxDays = currentMonth.dayOfMonth().getMaximumValue();

            int lastIndex = sectionFirstIndex + maxDays;
            lastIndex = (lastIndex > sectionData.size()) ? sectionData.size() : lastIndex;
            sections.add(new GraphSection(
                    sectionData.subList(sectionFirstIndex, lastIndex),
                    Lists.newArrayList(title),
                    highlightedValues,
                    highlightTitle
            ));
            sectionFirstIndex += maxDays;
        }

        return sections;
    }


    public static List<Float> padSectionData(final List<Float> data,
                                             final DateTime today,
                                             final DateTime firstDataDateTime, final DateTime lastDataDateTime,
                                             final TimeScale timeScale,
                                             final boolean padDayOfWeek,
                                             final Optional<DateTime> optionalCreated) {
        final List<Float> sectionData = Lists.newArrayList();

        final int numDays = timeScale.getDays();

        // fill in missing days first, include firstDate, always gated by account-creation date
        DateTime firstDate = today.minusDays(numDays);
        final DateTime accountCreatedDate;
        if (optionalCreated.isPresent()) {
            accountCreatedDate = optionalCreated.get();
        } else {
            accountCreatedDate = firstDate;
        }

        // if first-date in weekly calendar view is before account-creation,
        // set first-date as account-created date to prevent an extra row of nulls
        if (timeScale == TimeScale.LAST_WEEK && firstDate.isBefore(accountCreatedDate)) {
            firstDate = accountCreatedDate;
        }

        // for accounts created just after midnight, the first night-of will be one day before account-created
        if (firstDataDateTime.isBefore(firstDate)) {
            firstDate = firstDataDateTime;
        }

        final Days missingDays = Days.daysBetween(firstDate, firstDataDateTime);
        if (missingDays.getDays() > 0) {
            for (int day = 0; day < missingDays.getDays(); day ++) {
                final DateTime thisDayDate = firstDate.plusDays(day);
                if (thisDayDate.isBefore(accountCreatedDate)) {
                    sectionData.add(0,null);
                } else {
                    sectionData.add(GraphSection.MISSING_VALUE);
                }
            }
        }

        // pad front with nulls
        if (padDayOfWeek) {
            final int firstDateDOW = firstDate.getDayOfWeek();
            if (firstDateDOW < DateTimeConstants.SUNDAY) {
                for (int day = 0; day < firstDateDOW; day++) {
                    sectionData.add(0, null);
                }
            }
        }

        sectionData.addAll(data);

        // add missing values at the end
        final Days endMissingDays = Days.daysBetween(lastDataDateTime, today.minusDays(1));
        if (endMissingDays.getDays() > 0) {
            for (int day = 0; day < endMissingDays.getDays(); day ++) {
                sectionData.add(GraphSection.MISSING_VALUE);
            }
        }

        // pad ends with nulls
        if (padDayOfWeek) {
            final int todayDOW = today.getDayOfWeek();
            if (todayDOW < DateTimeConstants.SUNDAY) {
                for (int day = todayDOW; day < DateTimeConstants.SUNDAY; day++) {
                    sectionData.add(null);
                }
            }
        }
        return sectionData;
    }
}

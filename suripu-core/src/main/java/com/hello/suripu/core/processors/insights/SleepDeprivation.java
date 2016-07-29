package com.hello.suripu.core.processors.insights;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.*;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
/**
 * Created by jarredheinrich on 7/8/16.
 */
public class SleepDeprivation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepDeprivation.class);

    private static final int N_NIGHTS= 4;
    private static final int N_HISTORIC_NIGHTS = 28;
    private static final int N_HISTORIC_NIGHTS_MIN = 14; //minimal number of nights to calculate avg sleep dur
    private static final int DURATION_DIFF_THRESHOLD = 60; //minimal difference between avg deprived sleep and avg sleep for user

    public static Optional<InsightCard> getInsights(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final AccountReadDAO accountReadDAO, final Long accountId, final DateTime queryEndDate) {
        //ideal sleep duration
        final Optional<Account> optionalAccount = accountReadDAO.getById(accountId);
        final int userAge = (optionalAccount.isPresent()) ? DateTimeUtil.getDateDiffFromNowInDays(optionalAccount.get().DOB) / 365 : 0;
        final SleepDuration.recommendation idealHours = SleepDuration.getSleepDurationRecommendation(userAge);
        final int minSleepDurationMins = idealHours.absoluteMinHours * 60;
        final int idealSleepDurationHours = (idealHours.minHours + idealHours.maxHours )/2;
        //agg stat query dates
        final DateTime queryStartDate = queryEndDate.minusDays(N_NIGHTS - 1);
        final DateTime queryEndDateLastMonth = queryEndDate.minusDays(N_NIGHTS);
        final DateTime queryStartDateLastMonth = queryEndDate.minusDays(N_NIGHTS -1 + N_HISTORIC_NIGHTS);
        final String queryEndDateString = DateTimeUtil.dateToYmdString(queryEndDate);
        final String queryStartDateString = DateTimeUtil.dateToYmdString(queryStartDate);
        final String queryStartDateLastMonthString = DateTimeUtil.dateToYmdString(queryStartDateLastMonth);
        final String queryEndDateLastMonthString = DateTimeUtil.dateToYmdString(queryEndDateLastMonth);
        final List<AggregateSleepStats> sleepStatsLastWeek = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartDateString, queryEndDateString);
        final List<AggregateSleepStats> sleepStatsLastMonth = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartDateLastMonthString, queryEndDateLastMonthString);

        final Optional<InsightCard> card = processSleepDeprivationData(accountId, minSleepDurationMins, idealSleepDurationHours, sleepStatsLastWeek, sleepStatsLastMonth);
        return card;
    }

    public static Optional<InsightCard> processSleepDeprivationData(final Long accountId, final int minSleepDurationMins, final int idealSleepDurationHours, final List<AggregateSleepStats> sleepStatsLastWeek,final List<AggregateSleepStats> sleepStatsLastMonth){
        //gets duration and number of consecutive nights from agg_sleepstats
        int totalSleepDurationLastFourNights = 0;
        int numSleepDeprivedNights = 0;

        // initial check for number of nights in db
        if (sleepStatsLastWeek.size() < N_NIGHTS){
            LOGGER.debug("action=insight-ineligible insight=sleep-deprivation reason=not-enough-nights account_id={} num-nights={}", accountId, numSleepDeprivedNights);
            return Optional.absent();
        }

        for (final AggregateSleepStats stat : sleepStatsLastWeek){
            if (stat.sleepStats.sleepDurationInMinutes >= minSleepDurationMins){
                break;
            }
            numSleepDeprivedNights+=1;
            totalSleepDurationLastFourNights += stat.sleepStats.sleepDurationInMinutes;
        }

        //gets cases of not enough consecutive nights
        if (numSleepDeprivedNights < N_NIGHTS){
            LOGGER.debug("action=insight-ineligible insight=sleep-deprivation reason=not-enough-sleep-deprived-nights account_id={} num-nights={}", accountId, numSleepDeprivedNights);
            return Optional.absent();
        }

        //gets 4 weeks prior to deprivation window for baseline sleep durations
        int sumSleepDurationsLastMonth = 0;
        for (final AggregateSleepStats stat : sleepStatsLastMonth){
            sumSleepDurationsLastMonth += stat.sleepStats.sleepDurationInMinutes;
        }

        // if less than 14 nights worth of data in previous month, user ineligible for insight - requires historical data for baseline/ sanity check
        if (sleepStatsLastMonth.size() < N_HISTORIC_NIGHTS_MIN){
            LOGGER.debug("action=insight-ineligible insight=sleep-deprivation reason=not-enough-historical-data account_id={} numHistoricalNights={}", accountId, sleepStatsLastMonth.size());
            return Optional.absent();
        }

        final int meanSleepDurationLastMonth = sumSleepDurationsLastMonth / sleepStatsLastMonth.size();
        final int meanSleepDurationLastFourNights = totalSleepDurationLastFourNights/ N_NIGHTS;

        //ineligible conditions
        if (meanSleepDurationLastFourNights >= meanSleepDurationLastMonth - DURATION_DIFF_THRESHOLD){
            //safeguard for users who appear chronically sleep deprived or have atypical sleep habits - weekly average is not greatly different than avg for previous month.
            LOGGER.debug("action=insight-ineligible insight=sleep-deprivation reason=sleep-deprivation-within-1-hour-of-mean-duration account_id={} mean-duration={} mean-duration-historic={}", accountId, meanSleepDurationLastFourNights, meanSleepDurationLastMonth);
            return Optional.absent();
        }

        final int meanSleepDebtAvg = meanSleepDurationLastMonth - meanSleepDurationLastFourNights;
        final Text text = SleepDeprivationMsgEN.getSleepDeprivationMessage(idealSleepDurationHours, meanSleepDebtAvg);
        final Optional<InsightCard> card =  Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
                InsightCard.Category.SLEEP_DEPRIVATION, InsightCard.TimePeriod.WEEKLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
        LOGGER.debug("insight=sleep-deprivation account_id={} meanSleepDuration={}", accountId, N_NIGHTS, meanSleepDurationLastFourNights);

        return card;
    }
}

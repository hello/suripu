package com.hello.suripu.app.resources.v1;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AppStatsDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AppStats;
import com.hello.suripu.core.models.AppUnreadStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.QuestionProcessor;
import com.hello.suripu.core.util.PATCH;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v1/app/stats")
public class AppStatsResource {
    private final AppStatsDAO appStatsDAO;
    private final InsightsDAODynamoDB insightsDAO;
    private final QuestionProcessor questionProcessor;
    private final AccountDAO accountDAO;
    private final TimeZoneHistoryDAODynamoDB tzHistoryDAO;

    public AppStatsResource(final AppStatsDAO appStatsDAO,
                            final InsightsDAODynamoDB insightsDAO,
                            final QuestionProcessor questionProcessor,
                            final AccountDAO accountDAO,
                            final TimeZoneHistoryDAODynamoDB tzHistoryDAO) {
        this.appStatsDAO = appStatsDAO;
        this.insightsDAO = insightsDAO;
        this.questionProcessor = questionProcessor;
        this.accountDAO = accountDAO;
        this.tzHistoryDAO = tzHistoryDAO;
    }


    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AppStats getLastViewed(@Scope(OAuthScope.APP_STATS) final AccessToken accessToken) {
        final Optional<DateTime> insightsLastViewed = appStatsDAO.getInsightsLastViewed(accessToken.accountId);
        return new AppStats(insightsLastViewed);
    }

    @Timed
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateLastViewed(@Scope(OAuthScope.APP_STATS) final AccessToken accessToken,
                                     @Valid final AppStats appStats) {
        if (appStats.insightsLastViewed.isPresent()) {
            final DateTime insightsLastViewed = appStats.insightsLastViewed.get();
            appStatsDAO.putInsightsLastViewed(accessToken.accountId, insightsLastViewed);
            return Response.status(Response.Status.ACCEPTED).build();
        }

        return Response.status(Response.Status.NOT_MODIFIED).build();
    }

    @Timed
    @GET
    @Path("/unread")
    @Produces(MediaType.APPLICATION_JSON)
    public AppUnreadStats unread(@Scope(OAuthScope.APP_STATS) final AccessToken accessToken) {
        final Long accountId = accessToken.accountId;

        final Optional<DateTime> insightsLastViewed = appStatsDAO.getInsightsLastViewed(accountId);
        final Optional<Boolean> hasUnreadInsights = insightsLastViewed.transform(new Function<DateTime, Boolean>() {
            @Override
            public Boolean apply(DateTime insightsLastViewed) {
                final Boolean chronological = false; // most recent first
                final DateTime queryDate = DateTime.now(DateTimeZone.UTC).plusDays(1); // matches InsightsResource
                final ImmutableList<InsightCard> insights = insightsDAO.getInsightsByDate(accountId, queryDate, chronological, 1);
                final Boolean hasUnread;
                if (!insights.isEmpty()) {
                    hasUnread = insights.get(0).timestamp.isAfter(insightsLastViewed);
                } else {
                    hasUnread = false;
                }
                return hasUnread;
            }
        });

        final Optional<Integer> accountAgeInDays = getAccountAgeInDays(accountId);
        final Optional<Boolean> hasUnansweredQuestions = accountAgeInDays.transform(new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer accountAgeInDays) {
                final int timeZoneOffset = getTimeZoneOffsetMillis(accountId);
                final DateTime today = DateTime.now(DateTimeZone.UTC).plusMillis(timeZoneOffset).withTimeAtStartOfDay();
                final List<Question> questions = questionProcessor.getQuestions(accountId, accountAgeInDays,
                        today, QuestionProcessor.DEFAULT_NUM_QUESTIONS, true);
                return !questions.isEmpty();
            }
        });

        return new AppUnreadStats(hasUnreadInsights.or(false), hasUnansweredQuestions.or(false));
    }


    private int getTimeZoneOffsetMillis(final Long accountId) {
        final Optional<TimeZoneHistory> tzHistory = this.tzHistoryDAO.getCurrentTimeZone(accountId);
        if (tzHistory.isPresent()) {
            return tzHistory.get().offsetMillis;
        }

        return TimeZoneHistory.FALLBACK_OFFSET_MILLIS;
    }

    private Optional<Integer> getAccountAgeInDays(final Long accountId) {
        final Optional<Account> accountOptional = this.accountDAO.getById(accountId);
        return accountOptional.transform(new Function<Account, Integer>() {
            @Override
            public Integer apply(Account account) {
                return account.getAgeInDays();
            }
        });
    }
}

package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AppStatsDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.AppStats;
import com.hello.suripu.core.models.AppUnreadStats;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.questions.QuestionCategory;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.QuestionProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class AppStatsResourceTests {
    private static final DateTime FIXED_NOW = new DateTime(2015, 9, 17, 12, 30, 0);
    private static final long ACCOUNT_ID = 42L;

    private final AccessToken accessToken;

    private AppStatsDAO appStatsDAO;
    private InsightsDAODynamoDB insightsDAO;
    private QuestionProcessor questionProcessor;
    private AccountDAO accountDAO;
    private AppStatsResource resource;

    public AppStatsResourceTests() {
        this.accessToken = new AccessToken.Builder()
                .withAppId(1L)
                .withAccountId(ACCOUNT_ID)
                .withCreatedAt(DateTime.now())
                .withExpiresIn(9000L)
                .withRefreshToken(UUID.randomUUID())
                .withToken(UUID.randomUUID())
                .withScopes(new OAuthScope[] {OAuthScope.INSIGHTS_READ})
                .build();
    }

    @Before
    public void setUp() {
        this.appStatsDAO = mock(AppStatsDAO.class);
        doReturn(Optional.absent())
                .when(appStatsDAO)
                .getQuestionsLastViewed(ACCOUNT_ID);
        this.insightsDAO = mock(InsightsDAODynamoDB.class);
        this.questionProcessor = mock(QuestionProcessor.class);
        this.accountDAO = mock(AccountDAO.class);
        final TimeZoneHistoryDAODynamoDB tzHistoryDAO = mock(TimeZoneHistoryDAODynamoDB.class);
        doReturn(Optional.absent())
                .when(tzHistoryDAO)
                .getCurrentTimeZone(ACCOUNT_ID);
        this.resource = new AppStatsResource(appStatsDAO, insightsDAO, questionProcessor, accountDAO, tzHistoryDAO);

        DateTimeUtils.setCurrentMillisFixed(FIXED_NOW.getMillis());
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }


    @Test
    public void updateLastViewedEmptyPayload() throws Exception {
        final AppStats badAppStats = new AppStats(Optional.<DateTime>absent(), Optional.<DateTime>absent());
        final Response response = resource.updateLastViewed(accessToken, badAppStats);
        assertThat(response.getStatus(), is(equalTo(Response.Status.NOT_MODIFIED.getStatusCode())));
    }

    @Test
    public void unreadWithUnreadInsights() {
        final DateTime nowUTC = DateTime.now(DateTimeZone.UTC);
        doReturn(Optional.absent())
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        final DateTime insightsLastViewed = nowUTC.minusDays(1);
        doReturn(Optional.of(insightsLastViewed))
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        // would prefer to mock the insight card, but it doesn't support
        // mocking public member variables (for the timestamp)
        final InsightCard fakeInsight = new InsightCard(
                1L,
                "fake",
                "fake body",
                InsightCard.Category.GENERIC,
                InsightCard.TimePeriod.ANNUALLY,
                insightsLastViewed.plusSeconds(1));
        final ImmutableList<InsightCard> fakeInsights = ImmutableList.of(fakeInsight);
        doReturn(fakeInsights)
                .when(insightsDAO)
                .getInsightsByDate(ACCOUNT_ID, nowUTC.plusDays(1), false, 1);
        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(true));
        assertThat(unread.hasUnansweredQuestions, is(false));
    }

    @Test
    public void unreadWithReadInsights() {
        final DateTime nowUTC = DateTime.now(DateTimeZone.UTC);
        doReturn(Optional.absent())
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        final DateTime insightsLastViewed = nowUTC.minusDays(1);
        doReturn(Optional.of(insightsLastViewed))
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        doReturn(ImmutableList.of())
                .when(insightsDAO)
                .getInsightsByDate(ACCOUNT_ID, nowUTC.plusDays(1), false, 1);

        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(false));
        assertThat(unread.hasUnansweredQuestions, is(false));
    }

    @Test
    public void unreadWithJustReadInsight() {
        final DateTime nowUTC = DateTime.now(DateTimeZone.UTC);
        doReturn(Optional.absent())
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        final DateTime insightsLastViewed = nowUTC.minusDays(1);
        doReturn(Optional.of(insightsLastViewed))
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        final InsightCard fakeInsight = new InsightCard(
                1L,
                "fake",
                "fake body",
                InsightCard.Category.GENERIC,
                InsightCard.TimePeriod.ANNUALLY,
                insightsLastViewed);
        final ImmutableList<InsightCard> fakeInsights = ImmutableList.of(fakeInsight);
        doReturn(fakeInsights)
                .when(insightsDAO)
                .getInsightsByDate(ACCOUNT_ID, nowUTC.plusDays(1), false, 1);
        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(false));
        assertThat(unread.hasUnansweredQuestions, is(false));
    }

    @Test
    public void unreadWithUnansweredQuestionsWithoutQuestionLastViewed() {
        doReturn(Optional.absent())
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        final Account fakeAccount = new Account.Builder()
                .withId(ACCOUNT_ID)
                .withCreated(FIXED_NOW.minusDays(1))
                .withEmail("john.everyman@theinter.net")
                .build();
        doReturn(Optional.of(fakeAccount))
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        @SuppressWarnings("unchecked")
        final List<Question> fakeQuestions = mock(List.class);
        doReturn(false).when(fakeQuestions).isEmpty();
        doReturn(fakeQuestions)
                .when(questionProcessor)
                .getQuestions(eq(ACCOUNT_ID), eq(fakeAccount.getAgeInDays()), any(DateTime.class),
                        eq(QuestionProcessor.DEFAULT_NUM_QUESTIONS), eq(true));

        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(false));
        assertThat(unread.hasUnansweredQuestions, is(true));
    }

    @Test
    public void unreadWithUnansweredQuestionsWithQuestionLastViewed() {
        final DateTime nowUTC = DateTime.now(DateTimeZone.UTC);
        doReturn(Optional.absent())
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        doReturn(Optional.of(nowUTC.minusDays(1)))
                .when(appStatsDAO)
                .getQuestionsLastViewed(ACCOUNT_ID);
        final Account fakeAccount = new Account.Builder()
                .withId(ACCOUNT_ID)
                .withCreated(FIXED_NOW.minusDays(1))
                .withEmail("john.everyman@theinter.net")
                .build();
        doReturn(Optional.of(fakeAccount))
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        @SuppressWarnings("unchecked")
        final List<Question> fakeQuestions = Lists.newArrayList(new Question(0, ACCOUNT_ID,
                                                                             "Question question",
                                                                             "en_US",
                                                                             Question.Type.CHOICE,
                                                                             Question.FREQUENCY.ONE_TIME,
                                                                             Question.ASK_TIME.ANYTIME,
                                                                             0, 0,
                                                                             nowUTC,
                                                                             Collections.<Choice>emptyList(),
                                                                             AccountInfo.Type.SLEEP_TEMPERATURE,
                                                                             nowUTC, QuestionCategory.NONE));
        doReturn(fakeQuestions)
                .when(questionProcessor)
                .getQuestions(eq(ACCOUNT_ID), eq(fakeAccount.getAgeInDays()), any(DateTime.class),
                              eq(QuestionProcessor.DEFAULT_NUM_QUESTIONS), eq(true));

        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(false));
        assertThat(unread.hasUnansweredQuestions, is(true));
    }

    @Test
    public void readWithAnsweredQuestionsWithQuestionLastViewed() {
        final DateTime nowUTC = DateTime.now(DateTimeZone.UTC);
        doReturn(Optional.absent())
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        doReturn(Optional.of(nowUTC.minusDays(1)))
                .when(appStatsDAO)
                .getQuestionsLastViewed(ACCOUNT_ID);
        final Account fakeAccount = new Account.Builder()
                .withId(ACCOUNT_ID)
                .withCreated(FIXED_NOW.minusDays(1))
                .withEmail("john.everyman@theinter.net")
                .build();
        doReturn(Optional.of(fakeAccount))
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        @SuppressWarnings("unchecked")
        final List<Question> fakeQuestions = Lists.newArrayList(new Question(0, ACCOUNT_ID,
                                                                             "Question question",
                                                                             "en_US",
                                                                             Question.Type.CHOICE,
                                                                             Question.FREQUENCY.ONE_TIME,
                                                                             Question.ASK_TIME.ANYTIME,
                                                                             0, 0,
                                                                             nowUTC,
                                                                             Collections.<Choice>emptyList(),
                                                                             AccountInfo.Type.SLEEP_TEMPERATURE,
                                                                             nowUTC.minusDays(1), QuestionCategory.NONE));
        doReturn(fakeQuestions)
                .when(questionProcessor)
                .getQuestions(eq(ACCOUNT_ID), eq(fakeAccount.getAgeInDays()), any(DateTime.class),
                              eq(QuestionProcessor.DEFAULT_NUM_QUESTIONS), eq(true));

        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(false));
        assertThat(unread.hasUnansweredQuestions, is(false));
    }

    @Test
    public void unreadWithAnsweredQuestions() {
        doReturn(Optional.absent())
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        final Account fakeAccount = new Account.Builder()
                .withId(ACCOUNT_ID)
                .withCreated(FIXED_NOW.minusDays(1))
                .withEmail("john.everyman@theinter.net")
                .build();
        doReturn(Optional.of(fakeAccount))
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        @SuppressWarnings("unchecked")
        final List<Question> fakeQuestions = mock(List.class);
        doReturn(true).when(fakeQuestions).isEmpty();
        doReturn(fakeQuestions)
                .when(questionProcessor)
                .getQuestions(eq(ACCOUNT_ID), eq(fakeAccount.getAgeInDays()), any(DateTime.class),
                        eq(QuestionProcessor.DEFAULT_NUM_QUESTIONS), eq(true));

        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(false));
        assertThat(unread.hasUnansweredQuestions, is(false));
    }
}

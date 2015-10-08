package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AppStatsDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AppStats;
import com.hello.suripu.core.models.AppUnreadStats;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.processors.QuestionProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
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
        final AppStats badAppStats = new AppStats(Optional.<DateTime>absent());
        final Response response = resource.updateLastViewed(accessToken, badAppStats);
        assertThat(response.getStatus(), is(equalTo(Response.Status.NOT_MODIFIED.getStatusCode())));
    }

    @Test
    public void unreadWithUnreadInsights() {
        doReturn(Optional.absent())
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        final DateTime insightsLastViewed = FIXED_NOW.minusDays(1);
        doReturn(Optional.of(insightsLastViewed))
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        doReturn(1)
                .when(insightsDAO)
                .getInsightCountAfterDate(ACCOUNT_ID, insightsLastViewed, 1);

        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(true));
        assertThat(unread.hasUnansweredQuestions, is(false));
    }

    @Test
    public void unreadWithReadInsights() {
        doReturn(Optional.absent())
                .when(accountDAO)
                .getById(ACCOUNT_ID);
        final DateTime insightsLastViewed = FIXED_NOW.minusDays(1);
        doReturn(Optional.of(insightsLastViewed))
                .when(appStatsDAO)
                .getInsightsLastViewed(ACCOUNT_ID);
        doReturn(0)
                .when(insightsDAO)
                .getInsightCountAfterDate(ACCOUNT_ID, insightsLastViewed, 1);

        final AppUnreadStats unread = resource.unread(accessToken);
        assertThat(unread.hasUnreadInsights, is(false));
        assertThat(unread.hasUnansweredQuestions, is(false));
    }

    @Test
    public void unreadWithUnansweredQuestions() {
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

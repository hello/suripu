package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.ObjectGraphRoot;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.AccountQuestionResponses;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import com.hello.suripu.core.models.Response;
import com.hello.suripu.core.models.SleepStats;
import com.librato.rollout.RolloutAdapter;
import com.librato.rollout.RolloutClient;
import dagger.Module;
import dagger.Provides;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by ksg on 09/19/14
 */

public class QuestionProcessorTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(QuestionProcessorTest.class);

    private final static int CHECK_SKIP_NUM = 5;

    private final DateTime today = DateTime.now().withTimeAtStartOfDay();
    private final static Long ACCOUNT_ID_PASS = 1L;
    private final static Long ACCOUNT_ID_FAIL = 2L;

    private static final int ANOMALY_QUESTION_ID = 20000;

    private QuestionProcessor questionProcessor;

    public final Map<String, Boolean> features = Maps.newHashMap();
    public void setFeature(final String feat,boolean on) {
        if (features.containsKey(feat) && !on) {
            features.remove(feat);
        }

        if (on) {
            features.put(feat,true);
        }
    }

    final public  RolloutAdapter rolloutAdapter = new RolloutAdapter() {

        @Override
        public boolean userFeatureActive(String feature, long userId, List<String> userGroups) {
            Boolean hasFeature = features.get(feature);

            if (hasFeature == null) {
                hasFeature = Boolean.FALSE;
            }

            LOGGER.info("userFeatureActive {}={}",feature,hasFeature);
            return hasFeature;
        }

        @Override
        public boolean deviceFeatureActive(String feature, String deviceId, List<String> userGroups) {
            Boolean hasFeature = features.get(feature);

            if (hasFeature == null) {
                hasFeature = Boolean.FALSE;
            }

            LOGGER.info("deviceFeatureActive {}={}",feature,hasFeature);
            return hasFeature;
        }
    };

    @Module(
            injects = QuestionProcessor.class,
            library = true
    )

    class RolloutLocalModule {
        @Provides
        @Singleton
        RolloutAdapter providesRolloutAdapter() {
            return rolloutAdapter;
        }

        @Provides @Singleton
        RolloutClient providesRolloutClient(RolloutAdapter adapter) {
            return new RolloutClient(adapter);
        }

    }

    @Before
    public void setUp() {

        ObjectGraphRoot.getInstance().init(new RolloutLocalModule());
        features.clear();

        final SleepStatsDAODynamoDB sleepStatsDAODynamoDB = mock(SleepStatsDAODynamoDB.class);
        when(sleepStatsDAODynamoDB.getTimeZoneOffset(ACCOUNT_ID_PASS)).thenReturn(Optional.of(0));
        when(sleepStatsDAODynamoDB.getSingleStat(ACCOUNT_ID_PASS, this.today.minusDays(1).toString())).thenReturn(Optional.of(new AggregateSleepStats(0L, this.today, 0, 0, "String", new MotionScore(0,0,0F,0,0), 0, 0, 0, new SleepStats(0,0,0,0,Boolean.TRUE,0, 0L, 0L, 0))));

        when(sleepStatsDAODynamoDB.getTimeZoneOffset(ACCOUNT_ID_FAIL)).thenReturn(Optional.of(0));
        when(sleepStatsDAODynamoDB.getSingleStat(ACCOUNT_ID_FAIL, this.today.minusDays(1).toString())).thenReturn(Optional.of(new AggregateSleepStats(0L, this.today, 0, 0, "String", new MotionScore(0,0,0F,0,0), 0, 0, 0, new SleepStats(0,0,0,0,Boolean.TRUE,0, 0L, 0L, 0))));

        final List<Question> questions = this.getMockQuestions();
        final QuestionResponseDAO questionResponseDAO = mock(QuestionResponseDAO.class);
        when(questionResponseDAO.getAllQuestions()).thenReturn(ImmutableList.copyOf(questions));


        final Timestamp nextAskTimePass = new Timestamp(today.minusDays(2).getMillis());
        when(questionResponseDAO.getNextAskTime(ACCOUNT_ID_PASS)).thenReturn(Optional.fromNullable(nextAskTimePass));

        final Timestamp nextAskTimeFail = new Timestamp(today.plusDays(2).getMillis());
        when(questionResponseDAO.getNextAskTime(ACCOUNT_ID_FAIL)).thenReturn(Optional.fromNullable(nextAskTimeFail));

        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 1, today, today.plusDays(1))).thenReturn(10L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 2, today, today.plusDays(1))).thenReturn(11L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 3, today, today.plusDays(1))).thenReturn(12L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 4, today, today.plusDays(1))).thenReturn(13L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 5, today, today.plusDays(1))).thenReturn(14L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 6, today, today.plusDays(1))).thenReturn(21L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 7, today, today.plusDays(1))).thenReturn(19L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 8, today, today.plusDays(1))).thenReturn(20L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 10000, today, today.plusDays(1))).thenReturn(15L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 10002, today, today.plusDays(1))).thenReturn(16L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 10003, today, today.plusDays(1))).thenReturn(17L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 9, today, today.plusDays(1))).thenReturn(21L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 10, today, today.plusDays(1))).thenReturn(22L);

        // anomaly question
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 20000, today, today.plusDays(1))).thenReturn(18L);

        final List<AccountQuestion> recentAnomalyQuestionsFail = Lists.newArrayList();
        recentAnomalyQuestionsFail.add(new AccountQuestion(1000L, ACCOUNT_ID_FAIL, 20000,
                today.minusDays(QuestionProcessor.DAYS_BETWEEN_ANOMALY_QUESTIONS - 2),
                today.minusDays(QuestionProcessor.DAYS_BETWEEN_ANOMALY_QUESTIONS - 2).plusHours(3)));
        when(questionResponseDAO.getRecentAskedQuestionByQuestionId(ACCOUNT_ID_FAIL, 20000, 1)).thenReturn(ImmutableList.copyOf(recentAnomalyQuestionsFail));

        final List<AccountQuestion> recentAnomalyQuestionsPass = Lists.newArrayList();
        recentAnomalyQuestionsPass.add(new AccountQuestion(1001L, ACCOUNT_ID_PASS, 20000,
                today.minusDays(QuestionProcessor.DAYS_BETWEEN_ANOMALY_QUESTIONS + 2),
                today.minusDays(QuestionProcessor.DAYS_BETWEEN_ANOMALY_QUESTIONS + 2).plusHours(3)));
        when(questionResponseDAO.getRecentAskedQuestionByQuestionId(ACCOUNT_ID_PASS, 20000, 1)).thenReturn(ImmutableList.copyOf(recentAnomalyQuestionsPass));


        final List<Integer> answeredIds = new ArrayList<>();
        answeredIds.add(5);
        answeredIds.add(4);
        answeredIds.add(1);
        answeredIds.add(2);
        final DateTime oneWeekAgo = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(7);
        when(questionResponseDAO.getBaseAndRecentAnsweredQuestionIds(ACCOUNT_ID_PASS, 10000, oneWeekAgo)).thenReturn(answeredIds);

        when(questionResponseDAO.getLastFewResponses(ACCOUNT_ID_PASS, 5)).thenReturn(ImmutableList.copyOf(this.getSkippedResponses()));

        // get existing questions and response
        when(questionResponseDAO.getQuestionsResponsesByDate(ACCOUNT_ID_PASS, today.plusDays(1)))
                .thenReturn(ImmutableList.copyOf(Collections.<AccountQuestionResponses>emptyList()));
        final QuestionProcessor.Builder builder = new QuestionProcessor.Builder()
                .withQuestionResponseDAO(questionResponseDAO)
                .withCheckSkipsNum(CHECK_SKIP_NUM)
                .withSleepStatsDAODynamoDB(sleepStatsDAODynamoDB)
                .withQuestions(questionResponseDAO);

        when(questionResponseDAO.getBaseAndRecentResponses(ACCOUNT_ID_PASS, Question.FREQUENCY.ONE_TIME.toSQLString(), oneWeekAgo))
                .thenReturn(ImmutableList.copyOf(Collections.<Response>emptyList()));

        // create an existing anomaly question to test feature flipper
        final AccountQuestionResponses unansweredQuestion = new AccountQuestionResponses(1L, ACCOUNT_ID_PASS, ANOMALY_QUESTION_ID, today, false, today);
        when(questionResponseDAO.getQuestionsResponsesByDate(ACCOUNT_ID_FAIL, today.plusDays(1)))
                .thenReturn(ImmutableList.copyOf(Lists.newArrayList(unansweredQuestion)));

        when(questionResponseDAO.getBaseAndRecentResponses(ACCOUNT_ID_FAIL, Question.FREQUENCY.ONE_TIME.toSQLString(), oneWeekAgo))
                .thenReturn(ImmutableList.copyOf(Collections.<Response>emptyList()));

        this.questionProcessor = builder.build();
    }

    private List<Response> getSkippedResponses() {
        final List<Response> responses = new ArrayList<>();

        final Optional<Boolean> skipTrue = Optional.fromNullable(true);
        final Optional<Boolean> skipFalse = Optional.fromNullable(false);
        final Optional<Integer> responseId = Optional.fromNullable(0);
        final DateTime created = DateTime.now(DateTimeZone.UTC);
        final DateTime askTime = created.withTimeAtStartOfDay().minusDays(7);
        responses.add(new Response(5L, ACCOUNT_ID_PASS, 5, "", responseId, skipTrue, created, 14L, Optional.fromNullable(Question.FREQUENCY.ONE_TIME), askTime.plusDays(4)));
        responses.add(new Response(4L, ACCOUNT_ID_PASS, 4, "", Optional.fromNullable(10), skipFalse, created, 13L, Optional.fromNullable(Question.FREQUENCY.ONE_TIME), askTime.plusDays(3)));
        responses.add(new Response(3L, ACCOUNT_ID_PASS, 3, "", responseId, skipTrue, created, 12L, Optional.fromNullable(Question.FREQUENCY.ONE_TIME), askTime.plusDays(2)));
        responses.add(new Response(2L, ACCOUNT_ID_PASS, 2, "", responseId, skipTrue, created, 11L, Optional.fromNullable(Question.FREQUENCY.ONE_TIME), askTime.plusDays(1)));
        responses.add(new Response(1L, ACCOUNT_ID_PASS, 1, "", responseId, skipTrue, created, 10L, Optional.fromNullable(Question.FREQUENCY.ONE_TIME), askTime));

        return responses;
    }


    private List<Question> getMockQuestions() {
        final List<Question> questions = new ArrayList<>();

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final int dependency = 0;
        final int parentId = 0;
        final long accountQId = 0L;

        final List<Integer> dependencyResponse = Lists.newArrayList();

        List<Choice> choices = new ArrayList<>();
        int qid = 1;
        choices.add(new Choice(1, "Hot", qid));
        choices.add(new Choice(2, "Cold", qid));
        choices.add(new Choice(3, "No Effect", qid));
        questions.add(new Question(qid, accountQId,
                "Do you sleep hot or cold", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices, AccountInfo.Type.NONE, now,
                QuestionCategory.ONBOARDING, dependencyResponse));

        List<Choice> choices2 = new ArrayList<>();
        qid = 2;
        choices2.add(new Choice(4, "Snore", qid));
        choices2.add(new Choice(5, "Sleep-talk", qid));
        choices2.add(new Choice(6, "None", qid));
        questions.add(new Question(qid, accountQId,
                "Do you snore or talk in your sleep", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices2, AccountInfo.Type.NONE, now,
                QuestionCategory.ONBOARDING, dependencyResponse));

        List<Choice> choices3 = new ArrayList<>();
        qid = 3;
        choices.add(new Choice(7, "Yes", qid));
        choices.add(new Choice(8, "Somewhat", qid));
        choices.add(new Choice(9, "No", qid));
        questions.add(new Question(qid, accountQId,
                "Are you a light sleeper?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices3, AccountInfo.Type.NONE, now,
                QuestionCategory.ONBOARDING, dependencyResponse));

        List<Choice> choices4 = new ArrayList<>();
        qid = 5;
        choices4.add(new Choice(10, "coffee", qid));
        choices4.add(new Choice(11, "tea", qid));
        choices4.add(new Choice(12, "red bull", qid));
        choices4.add(new Choice(14, "others", qid));
        choices4.add(new Choice(15, "none", qid));
        questions.add(new Question(qid, accountQId,
                "Do you drink caffeine?", "EN",
                Question.Type.CHECKBOX,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices4, AccountInfo.Type.NONE, now,
                QuestionCategory.NONE, dependencyResponse));

        List<Choice> choices5 = new ArrayList<>();
        qid = 6;
        choices5.add(new Choice(15, "everyday", qid));
        choices5.add(new Choice(16, ">4 times a week", qid));
        choices5.add(new Choice(17, ">2 times a week", qid));
        choices5.add(new Choice(18, "once a week", qid));
        choices5.add(new Choice(19, "nope", qid));
        questions.add(new Question(qid, accountQId,
                "Do you workout?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices5, AccountInfo.Type.NONE, now,
                QuestionCategory.NONE, dependencyResponse));

        List<Choice> choices6 = new ArrayList<>();
        qid = 10000;
        choices6.add(new Choice(23, "great", qid));
        choices6.add(new Choice(24, "ok", qid));
        choices6.add(new Choice(25, "poor", qid));
        questions.add(new Question(qid, accountQId,
                "How was your sleep?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.DAILY,
                Question.ASK_TIME.MORNING,
                dependency, parentId, now, choices6, AccountInfo.Type.NONE, now,
                QuestionCategory.NONE, dependencyResponse));

        List<Choice> choices7 = new ArrayList<>();
        qid = 10002;
        choices7.add(new Choice(30, "yes", qid));
        choices7.add(new Choice(31, "no", qid));
        questions.add(new Question(qid, accountQId,
                "Did you workout today?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.DAILY,
                Question.ASK_TIME.EVENING,
                dependency, parentId, now, choices7, AccountInfo.Type.NONE, now,
                QuestionCategory.NONE, dependencyResponse));

        List<Choice> choices8 = new ArrayList<>();
        qid = 10003;
        choices8.add(new Choice(32, "great", qid));
        choices8.add(new Choice(33, "normal", qid));
        choices8.add(new Choice(34, "shitty", qid));
        questions.add(new Question(qid, accountQId,
                "How are you feeling?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.DAILY,
                Question.ASK_TIME.AFTERNOON,
                dependency, parentId, now, choices8, AccountInfo.Type.NONE, now,
                QuestionCategory.NONE, dependencyResponse));

        List<Choice> choices9 = new ArrayList<>();
        qid = ANOMALY_QUESTION_ID;
        choices9.add(new Choice(35, "Yep", qid));
        choices9.add(new Choice(36, "No", qid));
        choices9.add(new Choice(37, "wtf", qid));
        questions.add(new Question(qid, accountQId,
                "Too much light huh?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.TRIGGER,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices9, AccountInfo.Type.NONE, now,
                QuestionCategory.ANOMALY_LIGHT, dependencyResponse));

        List<Choice> choices10 = new ArrayList<>();
        qid = 4;
        choices10.add(new Choice(38, "try", qid));
        choices10.add(new Choice(39, "try not", qid));
        choices10.add(new Choice(40, "go away", qid));
        questions.add(new Question(qid, accountQId,
                "Do you work on being a good person", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices10, AccountInfo.Type.NONE, now,
                QuestionCategory.ONBOARDING, dependencyResponse));

        List<Choice> choices11 = new ArrayList<>();
        qid = 7;
        choices11.add(new Choice(41, "Yes", qid));
        choices11.add(new Choice(42, "No", qid));
        questions.add(new Question(qid, accountQId,
                "Did you take a nap today?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.OCCASIONALLY,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices11, AccountInfo.Type.NONE, now,
                QuestionCategory.NONE, dependencyResponse));


        List<Choice> choices12 = new ArrayList<>();
        qid = 8;
        choices12.add(new Choice(43, "Yes", qid));
        choices12.add(new Choice(44, "No", qid));
        questions.add(new Question(qid, accountQId,
                "Did you take a nap today?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.OCCASIONALLY,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices12, AccountInfo.Type.NONE, now,
                QuestionCategory.NONE, dependencyResponse));

        List<Choice> choices13 = new ArrayList<>();
        qid = 9;
        choices13.add(new Choice(45, "great", qid));
        choices13.add(new Choice(46, "normal", qid));
        choices13.add(new Choice(47, "poor", qid));
        questions.add(new Question(qid, accountQId,
                "How was the weather?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.DAILY,
                Question.ASK_TIME.ANYTIME,
                5, parentId, now, choices13, AccountInfo.Type.NONE, now,
                QuestionCategory.NONE,dependencyResponse));

        return questions;
    }

    @Test
    public void testGetOnBoardingQuestions() {
        final int accountAge = 0; // zero-days
        final List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, 2, true);

        assertThat(questions.size(), is(4));

        for (int i = 0; i < questions.size(); i++) {
            LOGGER.debug("Questions {}", questions.get(i));
            assertThat(questions.get(i).id, is(i+1));
        }
    }

    @Test
    public void testGetNewbieQuestions() {
        // expects one base and one calibration if asking for two
        final int accountAge = 2;
        int numQ = 2;
        List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, numQ, true);
        assertThat(questions.size(), is(numQ));

        boolean foundBaseQ = false;
        boolean foundCalibrationQ = false;
        for (Question question : questions) {
            final Question.FREQUENCY questionFrequency = question.frequency;
            if (questionFrequency == Question.FREQUENCY.ONE_TIME) {
                foundBaseQ = true;
            } else if (questionFrequency == Question.FREQUENCY.DAILY) {
                foundCalibrationQ = true;
            }
        }
        assertThat(foundBaseQ, is(true));
        assertThat(foundCalibrationQ, is(true));

        // try getting three questions
        numQ = 3;
        questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, numQ, true);
        assertThat(questions.size(), is(numQ));
        int countBaseQ = 0;
        foundCalibrationQ = false;
        for (Question question : questions) {
            final Question.FREQUENCY questionFrequency = question.frequency;
            if (questionFrequency == Question.FREQUENCY.ONE_TIME) {
                countBaseQ++;
            } else if (questionFrequency == Question.FREQUENCY.DAILY) {
                foundCalibrationQ = true;
            }
        }
        assertThat(countBaseQ, is(2));
        assertThat(foundCalibrationQ, is(true));

        // get 9, should include one ongoing question
        numQ = 9;
        questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, numQ, true);
        foundBaseQ = false;
        boolean foundOngoing = false;
        foundCalibrationQ = false;
        for (Question question : questions) {
            final Question.FREQUENCY questionFrequency = question.frequency;
            if (questionFrequency == Question.FREQUENCY.ONE_TIME) {
                foundBaseQ = true;
            } else if (questionFrequency == Question.FREQUENCY.DAILY) {
                foundCalibrationQ = true;
            } else if (questionFrequency == Question.FREQUENCY.OCCASIONALLY) {
                foundOngoing = true;
            }
        }
        assertThat(foundBaseQ, is(true));
        assertThat(foundOngoing, is(true));
        assertThat(foundCalibrationQ, is(true));
    }

    @Test
    public void testGetOldieQuestions() {
        final int accountAge = 14;
        int numQ = 4;

        List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, numQ, true);

        for (int i = 0; i < questions.size(); i++) {
            LOGGER.debug("Questions {}", questions.get(i));
        }

        assertThat(questions.size(), is(numQ));

        boolean foundBaseQ = false;
        boolean foundCalibrationQ = false;
        boolean foundMorningQ = false;
        boolean foundAfternoonQ = false;
        boolean foundEveningQ = false;

        for (Question question : questions) {
            final Question.FREQUENCY questionFrequency = question.frequency;
            final Question.ASK_TIME questionAskTime = question.askTime;
            if (questionFrequency == Question.FREQUENCY.ONE_TIME) {
                foundBaseQ = true;
            } else if (questionFrequency == Question.FREQUENCY.DAILY) {
                foundCalibrationQ = true;
            }
            if (questionAskTime == Question.ASK_TIME.MORNING){
                foundMorningQ = true;
            }else if (questionAskTime == Question.ASK_TIME.AFTERNOON) {
                foundAfternoonQ = true;
            }else if (questionAskTime == Question.ASK_TIME.EVENING){
                foundEveningQ = true;
            }
        }
        assertThat(foundBaseQ, is(true));
        assertThat(foundCalibrationQ, is(true));
        assertThat(foundMorningQ, is (true));
        assertThat(foundAfternoonQ, is (false));
        assertThat(foundEveningQ, is (false));

    }

    @Test
    public void testGetQuestionsByAskTime() {
        //checks getOldieQuestions w/out check ask time

        final Integer morningQuestionId = 10000;
        final Integer afternoonQuestionId = 10003;
        final Integer eveningQuestionId = 10002;

        //8 am
        List<Integer> eligibleQuestions =this.questionProcessor.getQuestionsByAskTime(8);
        assertThat(eligibleQuestions.contains(morningQuestionId), is(true));
        assertThat(eligibleQuestions.contains(afternoonQuestionId), is(false));
        assertThat(eligibleQuestions.contains(eveningQuestionId), is(false));

        //2 pm
        eligibleQuestions =this.questionProcessor.getQuestionsByAskTime(14);
        assertThat(eligibleQuestions.contains(morningQuestionId), is(true));
        assertThat(eligibleQuestions.contains(afternoonQuestionId), is(true));
        assertThat(eligibleQuestions.contains(eveningQuestionId), is(false));

        //8 pm
        eligibleQuestions =this.questionProcessor.getQuestionsByAskTime(20);
        assertThat(eligibleQuestions.contains(morningQuestionId), is(true));
        assertThat(eligibleQuestions.contains(afternoonQuestionId), is(true));
        assertThat(eligibleQuestions.contains(eveningQuestionId), is(true));
    }

    @Test
    public void testSkips() {
        final int tzOffsetMillis = -26200000;
        final int days = this.questionProcessor.setNextAskDate(ACCOUNT_ID_PASS, tzOffsetMillis);

        // 4 skips, but only 1 since the last response, so skip-days = 1
        assertThat(days, is(0));
    }

    @Test
    public void testPauseQuestion() {
        final int numQ = 3;
        final int accountAge = 2;

        final List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID_FAIL, accountAge, this.today, numQ, true);

        assertThat(questions.size(), is(0));
    }

    @Test
    public void saveResponse() {
        final List<Choice> choices = new ArrayList<>();

        int qid = 10003;
        choices.add(new Choice(34, "shitty", qid));
        boolean saved = this.questionProcessor.saveResponse(ACCOUNT_ID_PASS, qid, 1L, choices);
        assertThat(saved, is(true));

        choices.clear();
        choices.add(new Choice(30, "no", qid));
        saved = this.questionProcessor.saveResponse(ACCOUNT_ID_FAIL, qid, 2L, choices);
        assertThat(saved, is(false));

        // test saving checkboxes
        choices.clear();
        qid = 5;
        choices.add(new Choice(10, "coffee", qid));
        choices.add(new Choice(11, "tea", qid));
        saved = this.questionProcessor.saveResponse(ACCOUNT_ID_PASS, qid, 3L, choices);
        assertThat(saved, is(true));

        choices.clear();
        choices.add(new Choice(10, "coffee", qid));
        choices.add(new Choice(19, "no", qid));
        saved = this.questionProcessor.saveResponse(ACCOUNT_ID_FAIL, qid, 4L, choices);
        assertThat(saved, is(false));

    }

    @Test
    public void failAnomalyQuestionRecentlyAsked() {
        final DateTime nightDate = today.withTimeAtStartOfDay().minusDays(1);
        final boolean result = this.questionProcessor.insertLightAnomalyQuestion(ACCOUNT_ID_FAIL, nightDate, today);
        assertThat(result, is(false));
    }

    @Test
    public void failAnomalyQuestionTooOld() {
        final DateTime nightDate = today.withTimeAtStartOfDay().minusDays(QuestionProcessor.ANOMALY_TOO_OLD_THRESHOLD);
        final boolean result = this.questionProcessor.insertLightAnomalyQuestion(ACCOUNT_ID_PASS, nightDate, today);
        assertThat(result, is(false));
    }

    @Test
    public void insertAnomalyQuestionSuccess() {
        final DateTime nightDate = today.withTimeAtStartOfDay().minusDays(1);
        final boolean result = this.questionProcessor.insertLightAnomalyQuestion(ACCOUNT_ID_PASS, nightDate, today);
        assertThat(result, is(true));
    }

    @Test
    public void noAnomalyQuestions() {
        setFeature(FeatureFlipper.QUESTION_ANOMALY_LIGHT_VISIBLE, false);
        final List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID_FAIL, 20, today, 1, false);
        boolean foundAnomaly = false;
        for (final Question question : questions) {
            if (question.category.equals(QuestionCategory.ANOMALY_LIGHT)) {
                foundAnomaly = true;
                break;
            }
        }
        assertThat(foundAnomaly, is(false));
    }
}
package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kingshy on 9/19/14.
 */
public class QuestionProcessorTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(QuestionProcessorTest.class);

    private final static int CHECK_SKIP_NUM = 5;

    private final DateTime today = DateTime.now().withTimeAtStartOfDay();
    private final static Long ACCOUNT_ID_PASS = 1L;
    private final static Long ACCOUNT_ID_FAIL = 2L;

    private QuestionProcessor questionProcessor;

    @Before
    public void setUp() {
        final List<Question> questions = this.getQuestions();

        final QuestionResponseDAO questionResponseDAO = mock(QuestionResponseDAO.class);

        when(questionResponseDAO.getAllQuestions()).thenReturn(ImmutableList.<Question>of().copyOf(questions));

        final Timestamp nextAskTimePass = new Timestamp(today.minusDays(2).getMillis());
        when(questionResponseDAO.getNextAskTime(ACCOUNT_ID_PASS)).thenReturn(Optional.fromNullable(nextAskTimePass));

        final Timestamp nextAskTimeFail = new Timestamp(today.plusDays(2).getMillis());
        when(questionResponseDAO.getNextAskTime(ACCOUNT_ID_FAIL)).thenReturn(Optional.fromNullable(nextAskTimeFail));

        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 1, today, today.plusDays(1))).thenReturn(10L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 2, today, today.plusDays(1))).thenReturn(11L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 3, today, today.plusDays(1))).thenReturn(12L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 4, today, today.plusDays(1))).thenReturn(13L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 5, today, today.plusDays(1))).thenReturn(14L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 10000, today, today.plusDays(1))).thenReturn(15L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 10002, today, today.plusDays(1))).thenReturn(16L);
        when(questionResponseDAO.insertAccountQuestion(ACCOUNT_ID_PASS, 10003, today, today.plusDays(1))).thenReturn(17L);
        questionProcessor = new QuestionProcessor(questionResponseDAO, CHECK_SKIP_NUM);
    }

    private List<Question> getQuestions() {
        final List<Question> questions = new ArrayList<>();

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final int dependency = 0;
        final int parentId = 0;
        final long accountQId = 0L;

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
                dependency, parentId, now, choices));

        choices.clear();
        qid = 2;
        choices.add(new Choice(4, "Snore", qid));
        choices.add(new Choice(5, "Sleep-talk", qid));
        choices.add(new Choice(6, "None", qid));
        questions.add(new Question(qid, accountQId,
                "Do you snore or talk in your sleep", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 3;
        choices.add(new Choice(7, "Yes", qid));
        choices.add(new Choice(8, "Somewhat", qid));
        choices.add(new Choice(9, "No", qid));
        questions.add(new Question(qid, accountQId,
                "Are you a light sleeper?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 4;
        choices.add(new Choice(10, "coffee", qid));
        choices.add(new Choice(11, "tea", qid));
        choices.add(new Choice(12, "red bull", qid));
        choices.add(new Choice(14, "others", qid));
        choices.add(new Choice(15, "none", qid));
        questions.add(new Question(qid, accountQId,
                "Do you drink caffeine?", "EN",
                Question.Type.CHECKBOX,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 5;
        choices.add(new Choice(15, "everyday", qid));
        choices.add(new Choice(16, ">4 times a week", qid));
        choices.add(new Choice(17, ">2 times a week", qid));
        choices.add(new Choice(18, "once a week", qid));
        choices.add(new Choice(19, "nope", qid));
        questions.add(new Question(qid, accountQId,
                "Do you workout?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 10000;
        choices.add(new Choice(23, "great", qid));
        choices.add(new Choice(24, "ok", qid));
        choices.add(new Choice(25, "poor", qid));
        questions.add(new Question(qid, accountQId,
                "How was your sleep?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.DAILY,
                Question.ASK_TIME.MORNING,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 10002;
        choices.add(new Choice(30, "yes", qid));
        choices.add(new Choice(31, "no", qid));
        questions.add(new Question(qid, accountQId,
                "Did you workout today?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.DAILY,
                Question.ASK_TIME.EVENING,
                5, parentId, now, choices));

        choices.clear();
        qid = 10003;
        choices.add(new Choice(32, "great", qid));
        choices.add(new Choice(33, "normal", qid));
        choices.add(new Choice(34, "shitty", qid));
        questions.add(new Question(qid, accountQId,
                "How are you feeling?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.OCCASIONALLY,
                Question.ASK_TIME.AFTERNOON,
                dependency, parentId, now, choices));

        return questions;
    }

    @Test
    public void getOnBoardingQuestions() {
        final int accountAge = 0; // zero-days
        final List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, 2);

        assertThat(questions.size(), is(3));

        for (int i = 0; i < questions.size(); i++) {
            LOGGER.debug("Questions {}", questions.get(i));
            assertThat(questions.get(i).id, is(i+1));
        }
    }

    @Test
    public void getNewbieQuestions() {
        // expects one base and one calibration if asking for two
        final int accountAge = 2;
        int numQ = 2;
        List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, numQ);
        assertThat(questions.size(), is(numQ));

        boolean foundBaseQ = false;
        boolean foundCalibrationQ = false;
        for (int i = 0; i < questions.size(); i++) {
            final Question.FREQUENCY qfreq = questions.get(i).frequency;
            if (qfreq == Question.FREQUENCY.ONE_TIME) {
                foundBaseQ = true;
            } else if (qfreq == Question.FREQUENCY.DAILY) {
                foundCalibrationQ = true;
            }
        }
        assertThat(foundBaseQ, is(true));
        assertThat(foundCalibrationQ, is(true));

        // try getting three questions
        numQ = 3;
        questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, numQ);
        assertThat(questions.size(), is(numQ));
        int countBaseQ = 0;
        foundCalibrationQ = false;
        for (int i = 0; i < questions.size(); i++) {
            final Question.FREQUENCY qfreq = questions.get(i).frequency;
            if (qfreq == Question.FREQUENCY.ONE_TIME) {
                countBaseQ++;
            } else if (qfreq == Question.FREQUENCY.DAILY) {
                foundCalibrationQ = true;
            }
        }
        assertThat(countBaseQ, is(2));
        assertThat(foundCalibrationQ, is(true));

        // get 7, should include one ongoing question
        numQ = 7;
        questions = this.questionProcessor.getQuestions(ACCOUNT_ID_PASS, accountAge, this.today, numQ);
        foundBaseQ = false;
        boolean foundOngoing = false;
        foundCalibrationQ = false;
        for (int i = 0; i < questions.size(); i++) {
            final Question.FREQUENCY qfreq = questions.get(i).frequency;
            if (qfreq == Question.FREQUENCY.ONE_TIME) {
                foundBaseQ = true;
            } else if (qfreq == Question.FREQUENCY.DAILY) {
                foundCalibrationQ = true;
            } else if (qfreq == Question.FREQUENCY.OCCASIONALLY) {
                foundOngoing = true;
            }
        }
        assertThat(foundBaseQ, is(true));
        assertThat(foundOngoing, is(true));
        assertThat(foundCalibrationQ, is(true));
    }

    @Test
    public void testOnBoardingQuestion() {

    }

    @Test
    public void testPauseQuestion() {
        final int numQ = 3;
        final int accountAge = 2;

        final List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID_FAIL, accountAge, this.today, numQ);

        assertThat(questions.size(), is(0));
    }

    @Test
    public void saveResponse() {
        final int qid = 10003;
        Choice choice = new Choice(34, "shitty", qid);
        boolean saved = this.questionProcessor.saveResponse(ACCOUNT_ID_PASS, qid, 1L, choice);
        assertThat(saved, is(true));

        Choice wrongChoice = new Choice(30, "no", qid);
        saved = this.questionProcessor.saveResponse(ACCOUNT_ID_FAIL, qid, 2L, wrongChoice);
        assertThat(saved, is(false));

    }

}
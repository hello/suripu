package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kingshy on 10/24/14.
 */
public class QuestionProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionProcessor.class);
    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");

    private static int MAX_SKIPS_ALLOWED = 8;
    private static int MAX_BASE_QUESTION_ID = 10000; // reserved ids for base, one-time questions in EN

    private final QuestionResponseDAO questionResponseDAO;

    private final ListMultimap<String, Question> availableQuestions;
    private final Map<Integer, Question> questionIdMap;

    private final int checkSkipsNum;

    // bitmaps to track if user has answered one-time question


    public QuestionProcessor(final QuestionResponseDAO questionResponseDAO, final int checkSkipsNum) {
        this.questionResponseDAO = questionResponseDAO;
        this.checkSkipsNum = checkSkipsNum;

        this.availableQuestions = ArrayListMultimap.create();
        this.questionIdMap = new HashMap<>();

        final ImmutableList<Question> allQuestions = this.questionResponseDAO.getAllQuestions();
        for (final Question question : allQuestions) {
            this.availableQuestions.put(question.frequency.toString(), question);
            this.questionIdMap.put(question.id, question);
        }
    }

    /**
     * Get a list of questions for the user, or pre-generate one
     * @param accountId
     * @param today
     * @param numQuestions
     * @return
     */
    public List<Question> getQuestions(final Long accountId, final DateTime today, final Integer numQuestions) {

        // check if user has skipped too many questions in the past.
        final boolean pauseQuestion = this.pauseQuestions(accountId, today);
        if (pauseQuestion) {
            return Collections.emptyList();
        }

        // check if we have already generated a list of questions
        final Map<Integer, Question> preGeneratedQuestions = this.getPreGeneratedQuestions(accountId, today, numQuestions);
        if (preGeneratedQuestions.size() >= numQuestions) {
            return new ArrayList<>(preGeneratedQuestions.values());
        }

        // get additional questions if needed
        final Integer getMoreNum = numQuestions - preGeneratedQuestions.size();
        List<Question> questions = this.getAdditionalQuestions(accountId, today, getMoreNum, preGeneratedQuestions.keySet());
        if (!preGeneratedQuestions.isEmpty()) {
            questions.addAll(new ArrayList<>(preGeneratedQuestions.values()));
        }

        return questions;
    }

    /**
     * Save response to a question
     * @param accountId
     * @param questionId
     * @param accountQuestionId
     * @param choice
     */
    public void saveResponse(final Long accountId, final int questionId, final Long accountQuestionId, final Choice choice) {

        //check if choice is a valid one before saving to DB
        final Question responseToQuestion = this.questionIdMap.get(questionId);
        if (responseToQuestion.choiceList.contains(choice)) {
            this.questionResponseDAO.insertResponse(accountId, questionId, accountQuestionId, choice.id);
        } else {
            // response choice is not associated with this question
            LOGGER.warn("Account {} response to {} is not a valid choice: {}", accountId, accountQuestionId, choice);
        }
    }

    /**
     * Record a skipped question, set the next ask-time for this user
     * @param accountId
     * @param questionId
     * @param accountQuestionId
     * @param tzOffsetMillis
     */
    public void skipQuestion(final Long accountId, final Integer questionId, final Long accountQuestionId, final int tzOffsetMillis) {

        // add skips to table
        this.questionResponseDAO.insertSkippedQuestion(accountId, questionId, accountQuestionId);

        // find out how many the user has skipped
        final List<Long> skippedIds = this.getConsecutiveSkipsCount(accountId);
        int skipCount = skippedIds.size();;
        if (skipCount == 0 || skippedIds.get(0) != accountQuestionId) {
            skipCount++; // last skip insert is not retrieved
        }

        // set a delay for next ask time
        skipCount = Math.max(skipCount, this.MAX_SKIPS_ALLOWED);
        final int skipDays = (int) Math.exp((double) skipCount / 2.0);

        final DateTime nextAskDate = DateTime.now(DateTimeZone.UTC).plusMillis(tzOffsetMillis).withTimeAtStartOfDay().plusDays(skipDays);
        this.questionResponseDAO.setNextAskTime(accountId, nextAskDate);
    }


    private List<Long> getConsecutiveSkipsCount(final Long accountId) {
        // get last N questions, and check for consecutive skips
        final List<Long> skippedIds = new ArrayList<>();

//        Optional<List<Response>> optionalResponses = this.questionResponseDAO.getLastFewResponses(accountId, this.checkSkipsNum);
//        if (optionalResponses.isPresent()) {
        final List<Response> responses = this.questionResponseDAO.getLastFewResponses(accountId, this.checkSkipsNum);
        if (true) {
            for (final Response response : responses) {
                if (!response.skip) {
                    // not a skip, bolt
                    break;
                }
                skippedIds.add(response.accountQuestionId);
            }
        }
        return skippedIds;
    }

    //TODO
    private List<Question> getAdditionalQuestions(final Long accountId, final DateTime today, final Integer numQuestions, final Set<Integer> seenIds) {
        // TODO: logic to choose question

        Random rnd = new Random();

        // for now, get a random, one-time question
        final Set<Integer> addedIds = new HashSet<>();
        addedIds.addAll(seenIds);

        final List<Question> questionsPool = this.availableQuestions.get(Question.FREQUENCY.ONE_TIME.toString());
        final int poolSize = questionsPool.size();

        int loop = 0;
        final DateTime expiration = today.plusDays(1);
        final List<Question> questions = new ArrayList<>();

        while (questions.size() < numQuestions) {
            final int qid = rnd.nextInt(poolSize); // next random question
            final Question question = questionsPool.get(qid);

            if (!addedIds.contains(question.id)) {
                try {
                    // insert into DB for later retrieval
                    // TODO: make this batch Insert
                    final Long accountQId = this.questionResponseDAO.insertAccountQuestion(accountId, question.id, today, expiration);
                    questions.add(Question.withAskTimeAccountQId(question, accountQId, today));
                } catch (UnableToExecuteStatementException exception) {
                    Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                    if (matcher.find()) {
                        LOGGER.debug("Question already exist");
                    }
                }
                addedIds.add(question.id);
            }

            loop++;
            if (loop >= poolSize) {
                break;
            }
        }
        return questions;
    }

    private List<Integer> getUserAnsweredBaseIds (final Long accountId) {
        final List<Integer> baseIds = this.questionResponseDAO.getAccountAnsweredBaseIds(accountId, this.MAX_BASE_QUESTION_ID);
        return baseIds;
    }

    private Map<Integer, Question> getPreGeneratedQuestions(final Long accountId, final DateTime today, final int numQuestions) {
        final DateTime expiration = today.plusDays(1);
        final ImmutableList<AccountQuestion> questionIds = this.questionResponseDAO.getAccountQuestions(accountId, expiration, numQuestions);
        if (questionIds.size() == 0) {
            return Collections.emptyMap();
        }

        Map<Integer, Question> questions = new HashMap<>();

        for (final AccountQuestion question : questionIds) {
            final Integer qid = question.questionId;
            if (!questions.containsKey(qid)) {
                final Long accountQId = question.id;
                questions.put(qid, Question.withAskTimeAccountQId(this.questionIdMap.get(qid), accountQId, today));
            }
        }

        return questions;
    }

    /**
     * Check if we need to pause questions for this account
     * @param accountId
     * @param today
     * @return
     */
    private boolean pauseQuestions(Long accountId, DateTime today) {
        final Optional<Timestamp> nextAskTimestamp = this.questionResponseDAO.getNextAskTime(accountId);
        if (nextAskTimestamp.isPresent()){
            final DateTime nextAskDatetime = new DateTime(nextAskTimestamp.get(), DateTimeZone.UTC);
            if (nextAskDatetime.isAfter(today)) {
                return true;
            }
        }
        return false;
    }

}

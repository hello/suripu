package com.hello.suripu.core.processors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.Question;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final QuestionResponseDAO questionResponseDAO;

    private final ListMultimap<String, Question> availableQuestions;
    private final Map<Integer, Question> questionIdMap;

    public QuestionProcessor(final QuestionResponseDAO questionResponseDAO) {
        this.questionResponseDAO = questionResponseDAO;

        this.availableQuestions = ArrayListMultimap.create();
        this.questionIdMap = new HashMap<>();

        final ImmutableList<Question> allQuestions = this.questionResponseDAO.getAllQuestions();
        for (final Question question : allQuestions) {
            this.availableQuestions.put(question.frequency.toString(), question);
            this.questionIdMap.put(question.id, question);
        }
    }

    public List<Question> getQuestions(final Long accountId, final DateTime today, final Integer numQuestions) {

        // check if user has skipped too many questions in the past.
        final boolean pauseQuestion = this.checkPauseQuestions(accountId, today);
        if (pauseQuestion) {
            return Collections.emptyList();
        }

        // check if we have already generated a list of questions
        final Map<Integer, Question> preGeneratedQuestions = this.getPreGeneratedQuestions(accountId, today);
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

    private Map<Integer, Question> getPreGeneratedQuestions(final Long accountId, final DateTime today) {
        final DateTime expiration = today.plusDays(1);
        final ImmutableList<AccountQuestion> questionIds = this.questionResponseDAO.getAccountQuestions(accountId, expiration);
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

    private boolean checkPauseQuestions(Long accountId, DateTime today) {
        // TODO
        return false;
    }

}

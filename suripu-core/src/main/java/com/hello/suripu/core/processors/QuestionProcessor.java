package com.hello.suripu.core.processors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.models.Question;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
        final List<Question> questions = new ArrayList<>();

        // check if we have already generated a list of questions
        final DateTime expiration = today.plusDays(1);
        final List<Integer> questionIds = this.questionResponseDAO.getAccountQuestions(accountId, expiration);

        final Set<Integer> seenIds = new HashSet<>();
        if (questionIds.size() > 0) {
            for (final Integer qid : questionIds) {
                if (!seenIds.contains(qid)) {
                    questions.add(this.questionIdMap.get(qid));
                    seenIds.add(qid);
                }
            }
        }

        if (questions.size() >= numQuestions) {
            return questions;
        }

        Random rnd = new Random();

        // TODO: logic to choose question
        // for now, get a one-time question
        final List<Question> q = this.availableQuestions.get(Question.FREQUENCY.ONE_TIME.toString());
        final int numQ = q.size();
        int loop = 0;

        while (questions.size() < numQuestions) {
            final int qid = rnd.nextInt(numQ);
            final Question question = q.get(qid);

            if (!seenIds.contains(question.id)) {
                try {
                    // insert into DB for later retrieval
                    this.questionResponseDAO.insertAccountQuestion(accountId, question.id, today, expiration);
                    questions.add(Question.withAskLocalTime(question, today));
                } catch (UnableToExecuteStatementException exception) {
                    Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                    if (matcher.find()) {
                        LOGGER.debug("Question already exist");
                    }
                }
            }

            loop++;
            if (loop >= numQ) {
                break;
            }
        }

        return questions;
    }

}

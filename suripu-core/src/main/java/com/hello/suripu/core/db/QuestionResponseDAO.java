package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.AccountQuestionMapper;
import com.hello.suripu.core.db.mappers.QuestionMapper;
import com.hello.suripu.core.db.mappers.ResponseMapper;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Response;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.sql.Timestamp;

/**
 * Created by kingshy on 10/24/14.
 */
public interface QuestionResponseDAO {

    @RegisterMapper(QuestionMapper.class)
    @SqlQuery("SELECT * FROM questions")
    ImmutableList<Question> getAllQuestions();

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO responses (account_id, question_id, account_question_id, response_id) VALUES " +
            "(:account_id, :question_id, :account_question_id, :response_id)")
    Long insertResponse(@Bind("account_id") long accountId,
                        @Bind("question_id") Integer questionId,
                        @Bind("account_question_id") Long accountQuestionId,
                        @Bind("response_id") Integer responseId);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO responses (account_id, question_id, account_question_id, skip) VALUES " +
            "(:account_id, :question_id, :account_question_id, TRUE)")
    Long insertSkippedQuestion(@Bind("account_id") long accountId,
                               @Bind("question_id") Integer questionId,
                               @Bind("account_question_id") Long accountQuestionId);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_questions " +
            "(account_id, question_id, created_local_utc_ts, expires_local_utc_ts) VALUES " +
            "(:account_id, :question_id, :created_local, :expires_local)")
    Long insertAccountQuestion(@Bind("account_id") long accountId,
                               @Bind("question_id") Integer questionId,
                               @Bind("created_local") DateTime createdLocal,
                               @Bind("expires_local") DateTime expiresLocal);

    @RegisterMapper(AccountQuestionMapper.class)
    @SqlQuery("SELECT * FROM account_questions AQ WHERE " +
            "account_id = :account_id AND expires_local_utc_ts >= :expiration AND AQ.id NOT IN " +
            "(SELECT account_question_id FROM responses ORDER BY id DESC LIMIT :question_limit) " +
            "ORDER BY id DESC")
    ImmutableList<AccountQuestion> getAccountQuestions(@Bind("account_id") long accountId,
                                                       @Bind("expiration") DateTime expiration,
                                                       @Bind("question_limit") int questionLimit);


    // TODO need to optimize and create index
    @RegisterMapper(ResponseMapper.class)
    @SqlQuery("SELECT 0 AS id, Q.account_id AS account_id, " +
            "Q.question_id AS question_id, " +
            "Q.id AS account_question_id, " +
            "R.skip AS skip, " +
            "R.created AS created, " +
            "Q.created_local_utc_ts AS ask_time  " +
            "FROM account_questions Q " +
            "LEFT OUTER JOIN responses R ON R.account_question_id = Q.id " +
            "WHERE Q.account_id = :account_id ORDER BY Q.id DESC LIMIT :limit")
    ImmutableList<Response> getLastFewResponses(@Bind("account_id") long accountId,
                                                          @Bind("limit") int limit);

    @SqlQuery("SELECT question_id FROM responses WHERE account_id = :account_id AND question_id < :max_base_id")
    ImmutableList<Integer> getAccountAnsweredBaseIds(@Bind("account_id") long accountId,
                                                     @Bind("max_base_id") int maxBaseId);

    @SingleValueResult
    @SqlQuery("SELECT next_ask_time_local_utc FROM account_question_ask_time WHERE account_id = :account_id ORDER BY id DESC LIMIT 1")
    Optional<Timestamp> getNextAskTime(@Bind("account_id") long account_id);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_question_ask_time (account_id, next_ask_time_local_utc) VALUES " +
            "(:account_id, :next_ask_time)")
    Long setNextAskTime(@Bind("account_id") long accountId,
                        @Bind("next_ask_time") DateTime nextAskTime);
}

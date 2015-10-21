package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.AccountQuestionMapper;
import com.hello.suripu.core.db.mappers.AccountQuestionResponsesMapper;
import com.hello.suripu.core.db.mappers.QuestionMapper;
import com.hello.suripu.core.db.mappers.RecentResponseMapper;
import com.hello.suripu.core.db.mappers.ResponseMapper;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.AccountQuestionResponses;
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
import java.util.List;

/**
 * Created by kingshy on 10/24/14.
 */
public interface QuestionResponseDAO extends QuestionResponseReadDAO {

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO responses (account_id, question_id, account_question_id, response_id, question_freq) VALUES " +
            "(:account_id, :question_id, :account_question_id, :response_id, CAST(:frequency AS FREQUENCY_TYPE) )")
    Long insertResponse(@Bind("account_id") long accountId,
                        @Bind("question_id") Integer questionId,
                        @Bind("account_question_id") Long accountQuestionId,
                        @Bind("response_id") Integer responseId,
                        @Bind("frequency") String frequency);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO responses (account_id, question_id, account_question_id, question_freq, skip) " +
            "SELECT :account_id AS account_id, :question_id AS question_id," +
            ":account_question_id AS account_question_id, " +
            "CAST(:frequency AS FREQUENCY_TYPE) AS question_freq, TRUE AS skip " +
            "WHERE EXISTS (SELECT id FROM account_questions WHERE id = :account_question_id\n" +
            "  AND account_id = :account_id)\n")
    Long insertSkippedQuestion(@Bind("account_id") long accountId,
                                 @Bind("question_id") Integer questionId,
                                 @Bind("account_question_id") Long accountQuestionId,
                                 @Bind("frequency") String frequency);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_questions " +
            "(account_id, question_id, created_local_utc_ts, expires_local_utc_ts) VALUES " +
            "(:account_id, :question_id, :created_local, :expires_local)")
    Long insertAccountQuestion(@Bind("account_id") long accountId,
                               @Bind("question_id") Integer questionId,
                               @Bind("created_local") DateTime createdLocal,
                               @Bind("expires_local") DateTime expiresLocal);

    @SqlUpdate("INSERT INTO account_questions " +
            "(account_id, question_id, created_local_utc_ts, expires_local_utc_ts) VALUES " +
            "(:account_id, 1, :created_local, :expires_local), " +
            "(:account_id, 2, :created_local, :expires_local), " +
            "(:account_id, 3, :created_local, :expires_local)")
    void insertAccountOnBoardingQuestions(@Bind("account_id") long accountId,
                               @Bind("created_local") DateTime createdLocal,
                               @Bind("expires_local") DateTime expiresLocal);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_question_ask_time (account_id, next_ask_time_local_utc) VALUES " +
            "(:account_id, :next_ask_time)")
    Long setNextAskTime(@Bind("account_id") long accountId,
                        @Bind("next_ask_time") DateTime nextAskTime);
}

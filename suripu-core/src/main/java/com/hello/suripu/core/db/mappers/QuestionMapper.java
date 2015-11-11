package com.hello.suripu.core.db.mappers;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kingshy on 10/24/14.
 */
public class QuestionMapper implements ResultSetMapper<Question> {
    @Override
    public Question map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Integer[] response_ids = (Integer []) r.getArray("responses_ids").getArray();
        final String[] response_text = (String []) r.getArray("responses").getArray();

        final Integer question_id = r.getInt("id");

        final List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < response_ids.length; i++) {
            choices.add(new Choice(response_ids[i], response_text[i], question_id));
        }

        // TODO: refactor this at some point
        DateTime createdLocal;
        try {
            int foundCreatedLocal = r.findColumn("created_local_utc_ts");
            createdLocal = new DateTime(r.getTimestamp("created_local_utc_ts"), DateTimeZone.UTC);
        } catch (SQLException error) {
            createdLocal = DateTime.now(DateTimeZone.UTC);
        }

        Long accountQuestionId = 0L; // for account_questions table id field

        final Question question = new Question(question_id,
                                               accountQuestionId,
                                               r.getString("question_text"),
                                               r.getString("lang"),
                                               Question.Type.fromString(r.getString("response_type")),
                                               Question.FREQUENCY.fromString(r.getString("frequency")),
                                               Question.ASK_TIME.fromString(r.getString("ask_time")),
                                               r.getInt("dependency"),
                                               r.getInt("parent_id"),
                                               createdLocal,
                                               choices,
                                               AccountInfo.Type.fromString(r.getString("account_info")),
                                               Optional.of(createdLocal));

        return question;
    }
}

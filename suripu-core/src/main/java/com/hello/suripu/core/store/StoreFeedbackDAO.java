package com.hello.suripu.core.store;

import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface StoreFeedbackDAO {

    // CREATE TABLE store_feedback (id SERIAL PRIMARY KEY, account_id BIGINT NOT NULL, question VARCHAR(255), response VARCHAR(255), created_at TIMESTAMP);
    @SqlUpdate("INSERT INTO store_feedback (account_id, question, response, created_at) VALUES(:account_id, :question, :response, now())")
    int save(@BindStoreFeedback final StoreFeedback storeFeedback);
}

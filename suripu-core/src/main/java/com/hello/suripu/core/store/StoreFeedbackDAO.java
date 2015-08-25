package com.hello.suripu.core.store;

import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface StoreFeedbackDAO {

    // CREATE TABLE store_feedback (id SERIAL PRIMARY KEY, account_id BIGINT NOT NULL, like VARCHAR(255), review BOOLEAN, created_at TIMESTAMP);
    @SqlUpdate("INSERT INTO store_feedback (account_id, likes, review, created_at) VALUES(:account_id, :like, :review, now())")
    int save(@BindStoreFeedback final StoreFeedback storeFeedback);
}

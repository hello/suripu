CREATE TABLE oauth_tokens(
    id BIGSERIAL PRIMARY KEY,
    access_token UUID,
    refresh_token UUID,
    expires_in INTEGER,
    created_at TIMESTAMP default current_timestamp,
    app_id INTEGER,
    account_id INTEGER,
    scopes int[]
);


CREATE UNIQUE INDEX uniq_access_token on oauth_tokens(access_token);
CREATE UNIQUE INDEX uniq_refresh_token on oauth_tokens(refresh_token);


GRANT ALL PRIVILEGES ON oauth_tokens TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE oauth_tokens_id_seq TO ingress_user;
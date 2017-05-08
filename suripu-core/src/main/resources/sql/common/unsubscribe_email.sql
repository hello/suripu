-- save unsubscribe email 2016-01-27
CREATE TABLE unsubscribe_email (
     id SERIAL PRIMARY KEY,
     account_id BIGINT default 0,
     email VARCHAR(255),
     created TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_unsubscribe_email on unsubscribe_email(email);

GRANT ALL PRIVILEGES ON unsubscribe_email TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE unsubscribe_email_id_seq TO ingress_user;

-- Note: after inserting emails into the table, we can populate account_id with this:
-- UPDATE unsubscribe_email UN SET account_id = S.aid FROM
--   (SELECT A.id AS aid, U.email AS email FROM unsubscribe_email U INNER JOIN accounts A ON A.email = U.email) S
--   WHERE UN.email=S.email;

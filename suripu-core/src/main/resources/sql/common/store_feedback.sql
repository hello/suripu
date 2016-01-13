
-- tracks if user likes the app, and if they want to leave a review for the app store

CREATE TABLE store_feedback (
  id SERIAL PRIMARY KEY,
  account_id BIGINT NOT NULL,
  likes VARCHAR(255),
  review BOOLEAN,
  created_at TIMESTAMP
);

GRANT ALL PRIVILEGES ON store_feedback TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE store_feedback_id_seq TO ingress_user;

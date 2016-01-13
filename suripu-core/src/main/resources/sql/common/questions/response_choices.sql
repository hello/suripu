-- tracks all the responses
CREATE TABLE response_choices (
  id SERIAL PRIMARY KEY,
  question_id INTEGER,
  response_text TEXT,
  created TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_question_id_response_text on response_choices(question_id, response_text);

GRANT ALL PRIVILEGES ON response_choices TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE response_choices_id_seq TO ingress_user;


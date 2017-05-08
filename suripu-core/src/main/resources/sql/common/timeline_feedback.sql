CREATE TABLE timeline_feedback(
  id SERIAL PRIMARY KEY,
  account_id BIGINT,
  day TIMESTAMP,
  event_type INTEGER,
  event_datetime TIMESTAMP,
  created TIMESTAMP
);

GRANT ALL PRIVILEGES ON timeline_feedback TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE timeline_feedback_id_seq TO ingress_user;


DROP TABLE timeline_feedback;

-- new schema

CREATE TABLE timeline_feedback(
  id SERIAL PRIMARY KEY,
  account_id BIGINT,
  date_of_night timestamp,
  old_time VARCHAR(10),
  new_time VARCHAR(10),
  event_type INTEGER,
  created TIMESTAMP
);

-- 2015-12-14 Jake adds a column to determine if feedback was "correct" time or not.
ALTER TABLE timeline_feedback ADD COLUMN is_correct boolean DEFAULT TRUE;

-- UPDATES TO timeline_feedback TABLE 2016-01-19
--on purpose, default value is null, we are going to go in an back-populate
ALTER TABLE timeline_feedback ADD COLUMN adjustment_delta_minutes INTEGER;

-- UPDATE NEW COLUMN IN TABLE
UPDATE timeline_feedback
  SET adjustment_delta_minutes = q3.corrected_time FROM
    (SELECT id, CASE
      WHEN timediff > 720 THEN timediff - 1440
      WHEN timediff < -720 THEN 1440 + timediff
      ELSE timediff
      END as corrected_time
      FROM
        (SELECT q1.id,(extract(epoch from t2) - extract(epoch from t1))/60 timediff FROM
          (SELECT id,
            to_timestamp(concat('1970-01-01 ',new_time),'YYY-MM-DD HH24:mi')::timestamp without time zone t2,
            to_timestamp(concat('1970-01-01 ',old_time),'YYY-MM-DD HH24:mi')::timestamp without timezone t1
            FROM timeline_feedback
          ) q1
        ) q2
      ) q3
  WHERE timeline_feedback.id = q3.id;


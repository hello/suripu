CREATE TABLE sense_colors (
  id SERIAL PRIMARY KEY,
  sense_id VARCHAR(64),
  color VARCHAR(64)
);

CREATE UNIQUE index sense_id_color on sense_colors(sense_id);

GRANT ALL PRIVILEGES ON sense_colors TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sense_colors_id_seq TO ingress_user;


create table pill_provision (id SERIAL PRIMARY KEY, sn VARCHAR(100), device_id VARCHAR(100), created TIMESTAMP);


GRANT ALL PRIVILEGES ON pill_provision TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE pill_provision_id_seq TO ingress_user;
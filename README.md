Endpoints

    POST    /in (com.hello.suripu.service.resources.ReceiveResource)
    POST    /in/simple (com.hello.suripu.service.resources.ReceiveResource)
    GET     /ping (com.hello.dropwizard.mikkusu.resources.PingResource)
    GET     /version (com.hello.dropwizard.mikkusu.resources.VersionResource)


Protobuf definition:


````
package hello;

option java_package = "com.hello.suripu.api.input";
option java_outer_classname = "InputProtos";
option optimize_for = SPEED;


message SensorSampleBatch {
    message  SensorSample {
        enum SensorType {
            AMBIENT_TEMPERATURE = 0;
            AMBIENT_HUMIDITY = 1;
            AMBIENT_LIGHT = 2;
            AMBIENT_DECIBELS = 3;
            AMBIENT_AIR_QUALITY = 4;
            GPS = 5;
            PHONE_ACCELERATION = 6;
            PHONE_STEP_COUNT = 7;
        }

        optional SensorType sensor_type = 1;
        optional int32 timestamp = 2;
        optional int32 value = 3;

    }

    repeated SensorSample samples = 1;
    optional string device_id = 2;
}

message SimpleSensorBatch {
    message GpsInfo {
        optional float latitude = 1;
        optional float longitude = 2;
        optional float accuracy = 3;
        optional float speed = 4;
    }

    message SimpleSensorSample {
        optional int64 timestamp = 1;
        optional float ambient_temperature = 2;
        optional float ambient_humidity = 3;
        optional float ambient_light = 4;
        optional float ambient_decibels = 5;
        optional float ambient_air_quality = 6;

        optional GpsInfo gps = 10;
    }

    optional string device_id = 1;
    repeated SimpleSensorSample samples = 2;

}
````



Don't forget to create database/table

````sql

CREATE TABLE sensor_samples (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT,
    sensor_id INT,
    ts TIMESTAMP,
    val INT
);

CREATE UNIQUE INDEX uniq_sample on sensor_samples(device_id, ts);

GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;
GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;

CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;
ALTER ROLE ingress_user REPLICATION;
````

and change db config in surpipu.dev.yml

```yaml
database:
  # the name of your JDBC driver
  driverClass: org.postgresql.Driver

  # the username
  user: tim

  # the password
  password: hello ingress user

  # the JDBC URL
  url: jdbc:postgresql://localhost:5432/tim
````
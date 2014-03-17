Endpoints

    POST    /in (com.hello.suripu.service.resources.ReceiveResource)
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
````
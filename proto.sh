#!/bin/sh
protoc --java_out=suripu-api/src/main/java suripu-api/src/main/resources/protos/Input.proto
protoc --java_out=suripu-api/src/main/java suripu-api/src/main/resources/protos/Logging.proto
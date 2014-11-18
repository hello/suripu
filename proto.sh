#!/bin/sh
protoc --proto_path=suripu-api/src/main/resources/protos/. --java_out=suripu-api/src/main/java suripu-api/src/main/resources/protos/*.proto

protoc --proto_path=suripu-api/src/main/resources/protos/ --python_out=. suripu-api/src/main/resources/protos/*.proto

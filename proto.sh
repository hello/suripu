#!/bin/sh
protoc --java_out=suripu-api/src/main/java suripu-api/src/main/resources/protos/*.proto
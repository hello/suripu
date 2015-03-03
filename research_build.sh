#!/bin/sh

git checkout research
git pull
mvn clean test package

s3cmd put suripu-research/suripu-research.prod.yml s3://hello-research-deploy/suripu-research.prod.yml
s3cmd put suripu-research/target/suripu-research-*-SNAPSHOT.jar s3://hello-research-deploy/suripu-research.jar
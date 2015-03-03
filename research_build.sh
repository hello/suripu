#!/bin/sh

git checkout research
git pull
mvn clean test verify package

scp suripu-research/target/suripu-research-*-SNAPSHOT.jar build@ip-10-0-0-20.ec2.internal:/build/suripu-research.jar
scp suripu-research/suripu-research.prod.yml build@ip-10-0-0-20.ec2.internal:/build

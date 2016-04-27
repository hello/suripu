#!/bin/sh

if [ -z "$1" ]
  then
    echo "[ERROR] \xE2\x9A\xA0 Missing version number"
    exit 1
fi

VERSION=$1

# pardon the copy pasta but I don't know shit about bash

# prod

s3cmd put suripu-queue/suripu-queue.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-queue/$VERSION/suripu-queue.prod.yml


# staging
s3cmd put suripu-queue/suripu-queue.staging.yml s3://hello-deploy/configs/com/hello/suripu/suripu-queue/$VERSION/suripu-queue.staging.yml

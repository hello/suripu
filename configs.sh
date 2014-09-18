#!/bin/sh

VERSION=$1

s3cmd put suripu-app/suripu-app.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-app/$VERSION/suripu-app.prod.yml
s3cmd put suripu-service/suripu-service.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-service/$VERSION/suripu-service.prod.yml
s3cmd put suripu-workers/pillscorer.prod.yml s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pillscorer.prod.yml
